package com.group01.service;

import com.group01.dto.BookingDetailResponse;
import com.group01.dto.BookingVehicleResponse;
import com.group01.dto.BookingViewResponse;
import com.group01.enums.BookingStatus;
import com.group01.repository.BookingManagementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** New M4 unit tests; do not confuse with historical Excel case results. */
class BookingServiceTest {
    private BookingManagementRepository repository;
    private BookingNotificationService notifications;
    private BookingService service;

    @BeforeEach
    void setup() {
        repository = mock(BookingManagementRepository.class);
        notifications = mock(BookingNotificationService.class);
        service = new BookingService(repository, notifications, mock(OnlineDepositService.class));
    }

    private BookingViewResponse booking(Long userId, LocalDate date, String status) {
        return new BookingViewResponse(15L, userId, "Customer",
                date, date.plusDays(2), 1L, 1L, "Main branch",
                new BigDecimal("1000000.00"), new BigDecimal("300000.00"),
                "AT_PICKUP", status, LocalDateTime.now(), null, null);
    }

    @Test
    void memberCannotReadOtherUsersBooking() {
        when(repository.findDetails(15L)).thenReturn(new BookingDetailResponse(
                booking(2L, LocalDate.now().plusDays(5), "Pending"), List.of()));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.memberDetails(1L, 15L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void memberCannotCancelOtherUsersBooking() {
        when(repository.lockById(15L)).thenReturn(booking(2L, LocalDate.now().plusDays(5), "Pending"));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.memberCancel(1L, 15L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(repository, never()).updateStatus(anyLong(), any());
    }

    @Test
    void memberCanCancelFuturePendingBookingAndRequestRefund() {
        when(repository.lockById(15L)).thenReturn(booking(1L, LocalDate.now().plusDays(4), "Pending"));
        when(repository.findMemberEmail(1L)).thenReturn("member@example.com");
        var result = service.memberCancel(1L, 15L);
        assertEquals("Cancelled", result.get("status"));
        assertEquals(new BigDecimal("300000.00"), result.get("refundRequested"));
        verify(repository).recordPendingRefund(15L, new BigDecimal("300000.00"));
        verify(repository).updateStatus(15L, BookingStatus.CANCELLED);
    }

    @Test
    void cannotCancelCompletedBooking() {
        when(repository.lockById(15L)).thenReturn(booking(1L, LocalDate.now().plusDays(3), "Completed"));
        var e = assertThrows(ResponseStatusException.class, () -> service.memberCancel(1L, 15L));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(repository, never()).recordPendingRefund(anyLong(), any());
    }

    @Test
    void invalidAdminTransitionIsRejected() {
        when(repository.lockById(15L)).thenReturn(booking(1L, LocalDate.now().plusDays(3), "Pending"));
        var e = assertThrows(ResponseStatusException.class,
                () -> service.adminUpdateStatus(15L, BookingStatus.COMPLETED, "admin@example.com"));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void confirmedBookingCannotSkipPickupAndComplete() {
        when(repository.lockById(15L)).thenReturn(booking(1L, LocalDate.now().plusDays(3), "Confirmed"));
        var e = assertThrows(ResponseStatusException.class,
                () -> service.adminUpdateStatus(15L, BookingStatus.COMPLETED, "admin@example.com"));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void overlapBlocksAdminConfirmation() {
        var current = booking(1L, LocalDate.now().plusDays(3), "Pending");
        var vehicle = new BookingVehicleResponse("V001", "Car 1", current.pickupDate(),
                current.returnDate(), new BigDecimal("500000"), BigDecimal.ZERO, 2,
                new BigDecimal("1000000"));
        when(repository.lockById(15L)).thenReturn(current);
        when(repository.findDetails(15L)).thenReturn(new BookingDetailResponse(current, List.of(vehicle)));
        when(repository.hasConfirmationConflict(15L, "V001", current.pickupDate(), current.returnDate()))
                .thenReturn(true);
        var e = assertThrows(ResponseStatusException.class,
                () -> service.adminUpdateStatus(15L, BookingStatus.CONFIRMED, "admin@example.com"));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(repository, never()).updateStatus(15L, BookingStatus.CONFIRMED);
    }

    @Test
    void adminMayConfirmWhenNoConflict() {
        var current = booking(1L, LocalDate.now().plusDays(3), "Pending");
        var vehicle = new BookingVehicleResponse("V001", "Car 1", current.pickupDate(),
                current.returnDate(), new BigDecimal("500000"), BigDecimal.ZERO, 2,
                new BigDecimal("1000000"));
        when(repository.lockById(15L)).thenReturn(current);
        when(repository.findDetails(15L)).thenReturn(new BookingDetailResponse(current, List.of(vehicle)));
        when(repository.findMemberEmail(1L)).thenReturn("member@example.com");
        var result = service.adminUpdateStatus(15L, BookingStatus.CONFIRMED, "admin@example.com");
        assertEquals("Confirmed", result.get("status"));
        verify(repository).lockVehicle("V001");
        verify(repository).updateStatus(15L, BookingStatus.CONFIRMED);
        verify(notifications).notifyStatusChange("member@example.com", 15L, "Confirmed");
    }

    @Test
    void memberRequestsEarlyReturnAndAdminRecordsActualDate() {
        LocalDate today = LocalDate.now();
        when(repository.lockById(15L)).thenReturn(booking(1L, today.minusDays(1), "Picked up"));
        service.requestEarlyReturn(1L, 15L, today);
        verify(repository).saveReturnRequest(15L, today);
        verify(repository, never()).completeReturn(anyLong(), any());

        service.adminUpdateStatus(15L, BookingStatus.COMPLETED, "admin@example.com");
        verify(repository).completeReturn(15L, today);
    }
}

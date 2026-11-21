package com.morrow.vrs.booking;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.domain.enums.BookingStatus;
import com.morrow.vrs.integration.EmailNotificationAdapter;
import com.morrow.vrs.integration.PaymentGateway;
import com.morrow.vrs.repository.BookingRepository;
import com.morrow.vrs.repository.BookingStatusHistoryRepository;
import com.morrow.vrs.service.AdminBookingService;
import com.morrow.vrs.service.AvailabilityService;
import com.morrow.vrs.service.BookingCancellationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingModuleM4Test {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingStatusHistoryRepository historyRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private EmailNotificationAdapter emailAdapter;
    @Mock private AvailabilityService availabilityService;

    @InjectMocks private BookingCancellationService cancellationService;
    @InjectMocks private AdminBookingService adminBookingService;

    @Test
    void cancelBooking_MoreThan24HoursBeforePickup_ShouldRefundFullDeposit() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPickupDate(LocalDateTime.now().plusHours(30));
        booking.setDepositAmount(new BigDecimal("300000"));

        when(bookingRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(booking));

        var result = cancellationService.cancelByMember(10L, 1L, "Đổi lịch");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(new BigDecimal("300000"), result.refundedAmount());
        verify(paymentGateway, times(1)).refundDeposit(10L, new BigDecimal("300000"));
        verify(historyRepository, times(1)).save(any());
    }

    @Test
    void cancelBooking_WhenPickedUp_ShouldThrowException() {
        Booking booking = new Booking();
        booking.setId(11L);
        booking.setStatus(BookingStatus.PICKED_UP);
        booking.setPickupDate(LocalDateTime.now().plusHours(5));

        when(bookingRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class,
                () -> cancellationService.cancelByMember(11L, 1L, "Muốn hủy"));
        verify(paymentGateway, never()).refundDeposit(any(), any());
    }

    @Test
    void cancelBooking_ByDifferentUser_ShouldDenyAccess() {
        when(bookingRepository.findByIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> cancellationService.cancelByMember(10L, 99L, "Hack"));
    }

    @Test
    void adminUpdateStatus_InvalidTransition_ShouldFail() {
        Booking booking = new Booking();
        booking.setId(20L);
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(20L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class,
                () -> adminBookingService.updateBookingStatus(20L, BookingStatus.COMPLETED, 5L, "Nhảy cóc"));
        verify(bookingRepository, never()).save(any());
    }
} 
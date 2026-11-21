package com.morrow.vrs.service;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.domain.BookingDetail;
import com.morrow.vrs.domain.BookingStatusHistory;
import com.morrow.vrs.domain.enums.BookingStatus;
import com.morrow.vrs.integration.EmailNotificationAdapter;
import com.morrow.vrs.integration.PaymentGateway;
import com.morrow.vrs.repository.BookingRepository;
import com.morrow.vrs.repository.BookingStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final AvailabilityService availabilityService;
    private final PaymentGateway paymentGateway;
    private final EmailNotificationAdapter emailAdapter;

    @Transactional(readOnly = true)
    public Page<Booking> listBookings(BookingStatus status, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return bookingRepository.searchForAdmin(status, fromDate, toDate, pageable);
    }

    @Transactional
    public Booking updateBookingStatus(Long bookingId, BookingStatus targetStatus, Long adminUserId, String adminNote) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking ID: " + bookingId));

        BookingStatus currentStatus = booking.getStatus();

        // BR10: Kiểm tra luồng chuyển trạng thái hợp lệ
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                    String.format("Không thể chuyển trạng thái từ %s sang %s.", currentStatus, targetStatus));
        }

        // Nếu chuyển sang CONFIRMED, kiểm tra xung đột lịch xe qua AvailabilityService
        if (targetStatus == BookingStatus.CONFIRMED) {
            for (BookingDetail detail : booking.getDetails()) {
                boolean available = availabilityService.isVehicleAvailableExcludingBooking(
                        detail.getVehicle().getId(),
                        detail.getPickupDate(),
                        detail.getReturnDate(),
                        booking.getId()
                );
                if (!available) {
                    throw new IllegalStateException("Xe " + detail.getVehicle().getVehicleCode() + " bị trùng lịch với đơn khác.");
                }
            }
        }

        String finalNote = adminNote != null ? adminNote : "";

        // Chính sách hoàn cọc: Khi Admin hủy đơn (CANCELLED) -> hoàn 100% tiền cọc bất kể thời điểm hủy
        if (targetStatus == BookingStatus.CANCELLED) {
            BigDecimal deposit = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO;
            if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                paymentGateway.refundDeposit(booking.getId(), deposit);
            }
            String refundNote = "Admin hủy đơn: Hoàn 100% tiền cọc (" + deposit + ").";
            finalNote = finalNote.isBlank() ? refundNote : finalNote + " | " + refundNote;
            emailAdapter.sendBookingCancelledEmail(booking, deposit, refundNote);
        }

        booking.setStatus(targetStatus);
        Booking saved = bookingRepository.save(booking);

        // Ghi audit log admin, thời gian, trạng thái cũ/mới
        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(saved.getId())
                .oldStatus(currentStatus)
                .newStatus(targetStatus)
                .changedByUserId(adminUserId)
                .note(finalNote)
                .changedAt(LocalDateTime.now())
                .build());

        if (targetStatus != BookingStatus.CANCELLED) {
            emailAdapter.sendBookingStatusChangedEmail(saved, currentStatus, targetStatus);
        }

        return saved;
    }
}
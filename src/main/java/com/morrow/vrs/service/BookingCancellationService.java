package com.morrow.vrs.service;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.domain.BookingStatusHistory;
import com.morrow.vrs.domain.enums.BookingStatus;
import com.morrow.vrs.integration.EmailNotificationAdapter;
import com.morrow.vrs.integration.PaymentGateway;
import com.morrow.vrs.repository.BookingRepository;
import com.morrow.vrs.repository.BookingStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final PaymentGateway paymentGateway;
    private final EmailNotificationAdapter emailAdapter;

    public record CancellationResult(Long bookingId, BigDecimal refundedAmount, String refundPolicyNote) {}

    @Transactional
    public CancellationResult cancelByMember(Long bookingId, Long currentUserId, String reason) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền hủy đơn thuê này."));

        // BR09: Chỉ được hủy khi PENDING hoặc CONFIRMED
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể hủy đơn ở trạng thái PENDING hoặc CONFIRMED.");
        }

        LocalDateTime now = LocalDateTime.now();
        // BR09: Phải hủy trước thời điểm nhận xe
        if (!now.isBefore(booking.getPickupDate())) {
            throw new IllegalStateException("Không thể hủy đơn khi thời gian nhận xe đã bắt đầu.");
        }

        BookingStatus oldStatus = booking.getStatus();
        long hoursBeforePickup = Duration.between(now, booking.getPickupDate()).toHours();

        BigDecimal refundAmount = BigDecimal.ZERO;
        String policyNote;

        if (hoursBeforePickup >= 24) {
            // Hoàn 100% cọc nếu hủy >= 24 giờ trước pickup
            refundAmount = booking.getDepositAmount();
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentGateway.refundDeposit(booking.getId(), refundAmount);
            }
            policyNote = "Hoàn 100% tiền cọc do hủy trước giờ nhận xe >= 24 giờ.";
        } else {
            // DEC-03: Dưới 24 giờ chưa chốt tỷ lệ hoàn -> ghi nhận chờ xác minh
            policyNote = "DEC-03 PENDING: Hủy dưới 24 giờ trước giờ nhận xe, chờ quản trị viên xác minh chính sách cọc.";
        }

        // Cập nhật trạng thái (không xóa vật lý)
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Ghi audit log
        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(booking.getId())
                .oldStatus(oldStatus)
                .newStatus(BookingStatus.CANCELLED)
                .changedByUserId(currentUserId)
                .note("Khách hủy đơn. Lý do: " + reason + " | " + policyNote)
                .changedAt(now)
                .build());

        emailAdapter.sendBookingCancelledEmail(booking, refundAmount, policyNote);

        return new CancellationResult(booking.getId(), refundAmount, policyNote);
    }
}
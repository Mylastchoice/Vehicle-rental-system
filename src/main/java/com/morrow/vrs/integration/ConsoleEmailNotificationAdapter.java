package com.morrow.vrs.integration;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.domain.enums.BookingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class ConsoleEmailNotificationAdapter implements EmailNotificationAdapter {

    @Override
    public void sendBookingCancelledEmail(Booking booking, BigDecimal refundAmount, String policyNote) {
        String email = (booking.getUser() != null && booking.getUser().getEmail() != null)
                ? booking.getUser().getEmail()
                : "unknown@vrs.local";
        log.info("[EmailNotification] Gửi tới <{}> | Đơn #{} đã HỦY. Hoàn cọc: {} VND. Ghi chú: {}",
                email, booking.getId(), refundAmount, policyNote);
    }

    @Override
    public void sendBookingStatusChangedEmail(Booking booking, BookingStatus oldStatus, BookingStatus newStatus) {
        String email = (booking.getUser() != null && booking.getUser().getEmail() != null)
                ? booking.getUser().getEmail()
                : "unknown@vrs.local";
        log.info("[EmailNotification] Gửi tới <{}> | Đơn #{} đổi trạng thái từ {} -> {}",
                email, booking.getId(), oldStatus, newStatus);
    }
}
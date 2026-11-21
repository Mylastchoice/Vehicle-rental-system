package com.morrow.vrs.integration;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.domain.enums.BookingStatus;
import java.math.BigDecimal;

public interface EmailNotificationAdapter {
    void sendBookingCancelledEmail(Booking booking, BigDecimal refundAmount, String policyNote);
    void sendBookingStatusChangedEmail(Booking booking, BookingStatus oldStatus, BookingStatus newStatus);
}

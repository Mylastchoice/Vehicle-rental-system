package com.morrow.vrs;

import com.morrow.vrs.domain.Booking;
import com.morrow.vrs.domain.enums.BookingStatus;
import com.morrow.vrs.integration.EmailNotificationAdapter;
import com.morrow.vrs.integration.PaymentGateway;
import com.morrow.vrs.service.AvailabilityService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TestConfiguration
public class TestIntegrationStubConfig {

    @Bean
    public AvailabilityService availabilityService() {
        return (vehicleId, pickupDate, returnDate, excludeBookingId) -> true;
    }

    @Bean
    public PaymentGateway paymentGateway() {
        return (bookingId, amount) -> {};
    }

    @Bean
    public EmailNotificationAdapter emailNotificationAdapter() {
        return new EmailNotificationAdapter() {
            @Override
            public void sendBookingCancelledEmail(Booking booking, BigDecimal refundAmount, String policyNote) {}
            @Override
            public void sendBookingStatusChangedEmail(Booking booking, BookingStatus oldStatus, BookingStatus newStatus) {}
        };
    }
}
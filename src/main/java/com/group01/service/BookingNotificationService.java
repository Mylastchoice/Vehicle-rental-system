package com.group01.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Send messages only after a successful booking transaction commits. */
@Service
public class BookingNotificationService {
    private static final Logger log = LoggerFactory.getLogger(BookingNotificationService.class);
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String senderAddress;

    public BookingNotificationService(ObjectProvider<JavaMailSender> mailSender,
                                      @Value("${spring.mail.username:}") String senderAddress) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    public void notifyStatusChange(String email, Long bookingId, String newStatus) {
        if (email == null || email.isBlank()) return;
        Runnable send = () -> {
            if (senderAddress.isBlank()) {
                log.info("M4 email not configured: booking={} status={}", bookingId, newStatus);
                return;
            }
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) return;
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderAddress);
                message.setTo(email);
                message.setSubject("MORROW - Booking #" + bookingId);
                message.setText("Your booking #" + bookingId + " is now " + newStatus + ".");
                sender.send(message);
            } catch (Exception e) {
                log.warn("Could not email booking {}: {}", bookingId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send.run(); }
            });
        } else {
            send.run();
        }
    }
}

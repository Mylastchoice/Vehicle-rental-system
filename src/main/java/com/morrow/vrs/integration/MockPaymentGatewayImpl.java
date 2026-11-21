package com.morrow.vrs.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class MockPaymentGatewayImpl implements PaymentGateway {

    @Override
    public void refundDeposit(Long bookingId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[PaymentGateway] Không phát sinh giao dịch hoàn tiền cho đơn #{}", bookingId);
            return;
        }
        String txId = "RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[PaymentGateway] Đã hoàn tiền cọc {} VND cho đơn #{}. Mã giao dịch: {}", amount, bookingId, txId);
    }
}
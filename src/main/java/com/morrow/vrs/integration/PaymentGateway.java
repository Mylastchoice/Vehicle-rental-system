package com.morrow.vrs.integration;

import java.math.BigDecimal;

public interface PaymentGateway {
    void refundDeposit(Long bookingId, BigDecimal amount);
}

package com.group01.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Minimal shared booking view; M3 and M4 should agree on API fields. */
public record BookingSummaryResponse(
    Long bookingId,
    Long userId,
    LocalDate pickupDate,
    LocalDate returnDate,
    BigDecimal totalAmount,
    BigDecimal depositAmount,
    String status
) {}

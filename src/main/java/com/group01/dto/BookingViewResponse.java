package com.group01.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One booking shown in the member or administrator history (M4). */
public record BookingViewResponse(
        Long bookingId, Long userId, String renterName,
        LocalDate pickupDate, LocalDate returnDate,
        Long pickupBranchId, Long returnBranchId, String pickupLocation,
        BigDecimal totalAmount, BigDecimal depositAmount,
        String paymentMethod, String status, LocalDateTime createdAt,
        LocalDate requestedReturnDate, LocalDate actualReturnDate) {
}

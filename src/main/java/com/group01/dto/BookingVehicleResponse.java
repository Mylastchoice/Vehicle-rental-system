package com.group01.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Vehicle snapshot already stored in booking_details at checkout. */
public record BookingVehicleResponse(
        String vehicleCode, String vehicleName,
        LocalDate pickupDate, LocalDate returnDate,
        BigDecimal dailyPrice, BigDecimal discountRate,
        int rentalDays, BigDecimal subtotal) {
}

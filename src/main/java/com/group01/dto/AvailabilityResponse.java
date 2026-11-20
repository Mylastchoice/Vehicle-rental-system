package com.group01.dto;

import java.time.LocalDate;

/** Shared response contract. M3 owns availability business rules. */
public record AvailabilityResponse(
    String vehicleCode,
    LocalDate pickupDate,
    LocalDate returnDate,
    boolean available
) {}

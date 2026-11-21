package com.morrow.vrs.service;

import java.time.LocalDateTime;

public interface AvailabilityService {
    boolean isVehicleAvailableExcludingBooking(Long vehicleId, LocalDateTime pickupDate, LocalDateTime returnDate, Long excludeBookingId);
}

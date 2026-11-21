package com.morrow.vrs.service;

import com.morrow.vrs.domain.enums.BookingStatus;
import com.morrow.vrs.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAvailabilityServiceImpl implements AvailabilityService {

    private final BookingRepository bookingRepository;

    @Override
    public boolean isVehicleAvailableExcludingBooking(Long vehicleId, LocalDateTime pickupDate, LocalDateTime returnDate, Long excludeBookingId) {
        log.info("[AvailabilityService] Kiểm tra trùng lịch xe ID={} từ {} đến {} (loại trừ đơn #{})",
                vehicleId, pickupDate, returnDate, excludeBookingId);
        // Kiểm tra trong các đơn đang CONFIRMED hoặc IN_PROGRESS có trùng lịch xe này không
        return bookingRepository.findAll().stream()
                .filter(b -> excludeBookingId == null || !b.getId().equals(excludeBookingId))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .noneMatch(b -> b.getDetails() != null && b.getDetails().stream().anyMatch(d ->
                        d.getVehicle() != null
                                && d.getVehicle().getId().equals(vehicleId)
                                && d.getPickupDate() != null && d.getReturnDate() != null
                                && pickupDate.isBefore(d.getReturnDate())
                                && returnDate.isAfter(d.getPickupDate())
                ));
    }
}
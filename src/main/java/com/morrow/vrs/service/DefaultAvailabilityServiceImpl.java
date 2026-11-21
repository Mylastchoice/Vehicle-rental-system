package com.morrow.vrs.service;

import com.morrow.vrs.domain.enums.BookingStatus;
import com.morrow.vrs.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAvailabilityServiceImpl implements AvailabilityService {

    private final BookingRepository bookingRepository;

    // Các trạng thái đơn đang giữ lịch xe (Pending, Confirmed, Picked up)
    private static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES = Set.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.PICKED_UP
    );

    @Override
    public boolean isVehicleAvailableExcludingBooking(Long vehicleId, LocalDateTime pickupDate, LocalDateTime returnDate, Long excludeBookingId) {
        log.info("[AvailabilityService] Kiểm tra trùng lịch xe ID={} từ {} đến {} (loại trừ đơn #{})",
                vehicleId, pickupDate, returnDate, excludeBookingId);

        if (vehicleId == null || pickupDate == null || returnDate == null) {
            return false;
        }

        // Kiểm tra trùng lịch trong các đơn đang PENDING, CONFIRMED hoặc PICKED_UP
        return bookingRepository.findAll().stream()
                .filter(b -> excludeBookingId == null || !b.getId().equals(excludeBookingId))
                .filter(b -> b.getStatus() != null && ACTIVE_BOOKING_STATUSES.contains(b.getStatus()))
                .noneMatch(b -> b.getDetails() != null && b.getDetails().stream().anyMatch(d ->
                        d.getVehicle() != null
                                && d.getVehicle().getId().equals(vehicleId)
                                && d.getPickupDate() != null && d.getReturnDate() != null
                                && pickupDate.isBefore(d.getReturnDate())
                                && returnDate.isAfter(d.getPickupDate())
                ));
    }
}
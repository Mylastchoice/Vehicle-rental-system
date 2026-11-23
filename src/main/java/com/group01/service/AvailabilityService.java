
package com.group01.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.group01.repository.AvailabilityRepository;

@Service
public class AvailabilityService {
// service để xử lý logic liên quan đến tính khả dụng của xe
    private final AvailabilityRepository repository;

    public AvailabilityService(
            AvailabilityRepository repository) {
        this.repository = repository;
    }

    public boolean checkAvailability(
            String vehicleCode,
            LocalDate pickupDate,
            LocalDate returnDate) {// kiểm tra tính khả dụng của xe dựa trên mã xe và ngày thuê/trả

        // Kiem tra ma xe
        if (vehicleCode == null || vehicleCode.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Vehicle code is required"
            );
        }

        // Kiem tra ngay nhan va ngay tra
        if (pickupDate == null || returnDate == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Pickup date and return date are required"
            );
        }

        // Khong cho thue xe trong qua khu
        if (pickupDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Pickup date cannot be in the past"
            );
        }

        // Ngay tra phai sau ngay nhan
        if (!returnDate.isAfter(pickupDate)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Return date must be after pickup date"
            );
        }

        // Kiem tra lich thue trong PostgreSQL
        return repository.isAvailable(
            vehicleCode,
            pickupDate,
            returnDate
        );
    }
}


package com.group01.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.group01.dto.AvailabilityResponse;
import com.group01.service.AvailabilityService;

@RestController
@RequestMapping("/api/vehicles")
public class AvailabilityController { // controller để kiểm tra tính khả dụng của xe dựa trên mã xe và ngày thuê/trả

    private final AvailabilityService service;// service để xử lý logic liên quan đến tính khả dụng của xe

    public AvailabilityController(
            AvailabilityService service) {// constructor để khởi tạo AvailabilityController với AvailabilityService
        this.service = service;
    }

    @GetMapping("/{vehicleCode}/availability")// endpoint để kiểm tra tính khả dụng của xe dựa trên mã xe và ngày thuê/trả
    public AvailabilityResponse checkAvailability(
            @PathVariable("vehicleCode")// mã xe được truyền vào từ đường dẫn
            String vehicleCode,

            @RequestParam("pickupDate")// ngày thuê được truyền vào từ query parameter
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate pickupDate,

            @RequestParam("returnDate")// ngày trả được truyền vào từ query parameter
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate returnDate) {

        boolean available = service.checkAvailability(// kiểm tra tính khả dụng của xe
            vehicleCode,
            pickupDate,
            returnDate
        );

        return new AvailabilityResponse(
            vehicleCode,
            pickupDate,
            returnDate,
            available
        );
    }
}

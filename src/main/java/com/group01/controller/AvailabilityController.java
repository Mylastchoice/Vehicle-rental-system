
package com.group01.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.group01.dto.AvailabilityResponse;
import com.group01.service.AvailabilityService;

@RestController
@RequestMapping("/api/vehicles")
public class AvailabilityController {

    private final AvailabilityService service;

    public AvailabilityController(
            AvailabilityService service) {
        this.service = service;
    }

    @GetMapping("/{vehicleCode}/availability")
    public AvailabilityResponse checkAvailability(
            @PathVariable("vehicleCode")
            String vehicleCode,

            @RequestParam("pickupDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate pickupDate,

            @RequestParam("returnDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate returnDate) {

        boolean available = service.checkAvailability(
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

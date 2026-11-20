package com.group01.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.group01.service.CheckoutService;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    public record CheckoutRequest(
            String renterName,
            String renterPhone,
            String renterLicenseNumber,
            String pickupLocation,
            String paymentMethod) {
    }

    // POST /api/checkout/1
    @PostMapping("/{userId}")
    public Map<String, Object> checkout(
            @PathVariable Long userId,
            @RequestBody CheckoutRequest request) {
        return service.checkout(userId,
                request.renterName(), request.renterPhone(),
                request.renterLicenseNumber(),
                request.pickupLocation(), request.paymentMethod());
    }
}

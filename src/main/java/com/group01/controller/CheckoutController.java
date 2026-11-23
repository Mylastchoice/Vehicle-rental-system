package com.group01.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import com.group01.service.CheckoutService;
import com.group01.service.CurrentMemberService;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService service;
    private final CurrentMemberService currentMember;

    public CheckoutController(CheckoutService service, CurrentMemberService currentMember) {
        this.service = service;
        this.currentMember = currentMember;
    }

    public record CheckoutRequest(String renterName, String renterPhone,
            String renterLicenseNumber, String pickupLocation,
            String paymentMethod, String paymentOption) {
        public CheckoutRequest(String renterName, String renterPhone,
                String renterLicenseNumber, String pickupLocation, String paymentMethod) {
            this(renterName, renterPhone, renterLicenseNumber, pickupLocation, paymentMethod, "DEPOSIT");
        }
    }

    @PostMapping
    public Map<String, Object> checkout(Principal principal, @RequestBody CheckoutRequest request) {
        return service.checkout(currentMember.requireMemberId(principal),
                request.renterName(), request.renterPhone(), request.renterLicenseNumber(),
                request.pickupLocation(), request.paymentMethod(), request.paymentOption());
    }
}
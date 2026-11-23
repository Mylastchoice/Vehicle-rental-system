package com.group01.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import com.group01.service.CheckoutService;
import com.group01.service.CurrentMemberService;

@RestController// controller để xử lý các yêu cầu liên quan đến thanh toán và đặt xe
@RequestMapping("/api/checkout")// endpoint để xử lý các yêu cầu liên quan đến thanh toán và đặt xe
public class CheckoutController {// controller để xử lý các yêu cầu liên quan đến thanh toán và đặt xe
    private final CheckoutService service;// service để xử lý logic liên quan đến thanh toán và đặt xe
    private final CurrentMemberService currentMember;// service để lấy thông tin người dùng hiện tại

    public CheckoutController(CheckoutService service, CurrentMemberService currentMember) {
        this.service = service;
        this.currentMember = currentMember;
    }

    public record CheckoutRequest(String renterName, String renterPhone,
            String renterLicenseNumber, String pickupLocation, String paymentMethod) {}// record để nhận dữ liệu từ request body khi người dùng thực hiện thanh toán và đặt xe

    @PostMapping// endpoint để thực hiện thanh toán và đặt xe
    public Map<String, Object> checkout(Principal principal, @RequestBody CheckoutRequest request) {// thực hiện thanh toán và đặt xe cho người dùng hiện tại
        return service.checkout(currentMember.requireMemberId(principal),
                request.renterName(), request.renterPhone(), request.renterLicenseNumber(),
                request.pickupLocation(), request.paymentMethod());// gọi service để thực hiện thanh toán và đặt xe
    }
}

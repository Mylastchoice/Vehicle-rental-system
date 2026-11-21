package com.group01.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.group01.service.CartService;
import com.group01.service.CurrentMemberService;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    private final CartService service;
    private final CurrentMemberService currentMember;

    public CartController(CartService service, CurrentMemberService currentMember) {
        this.service = service;
        this.currentMember = currentMember;
    }

    public record AddItemRequest(String vehicleCode, LocalDate pickupDate, LocalDate returnDate) {}
    public record UpdateDatesRequest(LocalDate pickupDate, LocalDate returnDate) {}

    @GetMapping("/me")
    public Map<String, Object> getCart(Principal principal) {
        return service.getCart(currentMember.requireMemberId(principal));
    }

    @PostMapping("/me/items")
    public Map<String, Object> addItem(Principal principal, @RequestBody AddItemRequest request) {
        return service.addItem(currentMember.requireMemberId(principal),
                request.vehicleCode(), request.pickupDate(), request.returnDate());
    }

    @PutMapping("/me/items/{itemId}")
    public Map<String, Object> updateDates(Principal principal, @PathVariable Long itemId,
            @RequestBody UpdateDatesRequest request) {
        return service.updateDates(currentMember.requireMemberId(principal), itemId,
                request.pickupDate(), request.returnDate());
    }

    @DeleteMapping("/me/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(Principal principal, @PathVariable Long itemId) {
        service.removeItem(currentMember.requireMemberId(principal), itemId);
    }
}

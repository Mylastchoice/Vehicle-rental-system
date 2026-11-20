
package com.group01.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.group01.service.CartService;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    public record AddItemRequest(
        String vehicleCode,
        LocalDate pickupDate,
        LocalDate returnDate
    ) {}

    // Them xe hoac cap nhat ngay thue
    @PostMapping("/{userId}/items")
    public Map<String, Object> addItem(
            @PathVariable Long userId,
            @RequestBody AddItemRequest request) {

        return service.addItem(
            userId,
            request.vehicleCode(),
            request.pickupDate(),
            request.returnDate()
        );
    }

    // Xem gio hang
    @GetMapping("/{userId}")
    public Map<String, Object> getCart(
            @PathVariable Long userId) {

        return service.getCart(userId);
    }

    // Xoa xe khoi gio
    @DeleteMapping("/{userId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @PathVariable Long userId,
            @PathVariable Long itemId) {

        service.removeItem(userId, itemId);
    }
}

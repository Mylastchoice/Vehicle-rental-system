
package com.group01.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.group01.repository.CartRepository;

@Service
public class CartService {

    private final CartRepository repository;
    private final AvailabilityService availabilityService;

    public CartService(
            CartRepository repository,
            AvailabilityService availabilityService) {

        this.repository = repository;
        this.availabilityService = availabilityService;
    }

    private void checkUser(Long userId) {

        if (userId == null || userId <= 0
                || !repository.userExists(userId)) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
            );
        }
    }

    @Transactional
    public Map<String, Object> addItem(
            Long userId,
            String vehicleCode,
            LocalDate pickupDate,
            LocalDate returnDate) {

        checkUser(userId);

        // Su dung lai UC14 de kiem tra xe
        boolean available = availabilityService
            .checkAvailability(
                vehicleCode,
                pickupDate,
                returnDate
            );

        if (!available) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Vehicle is not available"
            );
        }

        Long cartId = repository.getOrCreateCart(userId);

        repository.saveItem(
            cartId,
            vehicleCode,
            pickupDate,
            returnDate
        );

        return getCart(userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCart(Long userId) {

        checkUser(userId);

        return Map.of(
            "userId", userId,
            "items", repository.getItems(userId)
        );
    }

    @Transactional
    public Map<String, Object> updateDates(Long userId, Long itemId,
            LocalDate pickupDate, LocalDate returnDate) {
        checkUser(userId);
        String vehicleCode = repository.findVehicleCodeForItem(userId, itemId);
        if (vehicleCode == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        if (!availabilityService.checkAvailability(vehicleCode, pickupDate, returnDate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle is not available");
        }
        if (repository.updateDates(userId, itemId, pickupDate, returnDate) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        return getCart(userId);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {

        checkUser(userId);

        int deleted = repository.deleteItem(userId, itemId);

        if (deleted == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cart item not found"
            );
        }
    }
}


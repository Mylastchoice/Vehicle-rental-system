package com.group01.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {
    CartController.class, CheckoutController.class, AvailabilityController.class,
    VehicleOptionsController.class, BookingController.class, BookingAdminController.class
})
public class RentalApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> respond(ResponseStatusException error) {
        String reason = error.getReason() == null ? "Request could not be processed" : error.getReason();
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", reason));
    }
}

package com.group01.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.group01.repository.CheckoutRepository;
import com.group01.repository.CheckoutRepository.CartLine;

@Service
public class CheckoutService {

    private final CheckoutRepository repository;
    private final AvailabilityService availabilityService;

    public CheckoutService(CheckoutRepository repository,
            AvailabilityService availabilityService) {
        this.repository = repository;
        this.availabilityService = availabilityService;
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " is required");
        }
    }

    /** VN mobile number: 0[35789]xxxxxxxx or +84[35789]xxxxxxxx. */
    static String validVietnamesePhone(String value) {
        if (value == null || !value.trim().matches("(?:0|\\+84)[35789]\\d{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid Vietnamese mobile phone number");
        }
        return value.trim();
    }

    private BigDecimal subtotal(CartLine line) {
        int days = Math.toIntExact(ChronoUnit.DAYS.between(
                line.pickupDate(), line.returnDate()));
        return line.dailyPrice()
                .multiply(BigDecimal.ONE.subtract(line.discountRate()))
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public Map<String, Object> checkout(Long userId, String renterName,
            String renterPhone, String licenseNumber,
            String pickupLocation, String paymentMethod) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid user ID");
        }
        requireText(renterName, "Renter name");
        String normalizedPhone = validVietnamesePhone(renterPhone);
        requireText(licenseNumber, "Driving license/ID number");
        requireText(pickupLocation, "Pickup location");
        requireText(paymentMethod, "Payment method");

        if ("ONLINE".equalsIgnoreCase(paymentMethod.trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                    "Online payment gateway is not integrated yet");
        }
        if (!"AT_PICKUP".equalsIgnoreCase(paymentMethod.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment method must be AT_PICKUP or ONLINE");
        }

        if (!repository.hasValidUserLicense(userId, licenseNumber.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User must be active and have a matching license/ID in profile");
        }

        Long cartId = repository.lockCart(userId);
        if (cartId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cart does not exist");
        }
        List<CartLine> items = repository.getCartLines(cartId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cart is empty");
        }

        LocalDate pickup = items.get(0).pickupDate();
        LocalDate returned = items.get(0).returnDate();
        long branchId = items.get(0).branchId();

        // Bookings lưu một khoảng ngày và một chi nhánh nhận xe.
        for (CartLine item : items) {
            if (!pickup.equals(item.pickupDate())
                    || !returned.equals(item.returnDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All vehicles must have the same rental dates");
            }
            if (branchId != item.branchId()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All vehicles must belong to the same pickup branch");
            }
        }

        // Khóa từng xe theo mã đã sắp xếp, tránh checkout đồng thời.
        for (CartLine item : items) {
            repository.lockVehicle(item.vehicleCode());
        }

        List<BigDecimal> subtotals = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine item : items) {
            if (!availabilityService.checkAvailability(item.vehicleCode(),
                    item.pickupDate(), item.returnDate())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Vehicle is not available: " + item.vehicleCode());
            }
            BigDecimal lineTotal = subtotal(item);
            subtotals.add(lineTotal);
            total = total.add(lineTotal);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal deposit = total.multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_UP);

        Long bookingId = repository.createBooking(userId, branchId,
                pickupLocation.trim(), pickup, returned,
                renterName.trim(), normalizedPhone,
                licenseNumber.trim(), total, deposit);

        for (int i = 0; i < items.size(); i++) {
            CartLine item = items.get(i);
            int days = Math.toIntExact(ChronoUnit.DAYS.between(
                    item.pickupDate(), item.returnDate()));
            repository.createBookingDetail(bookingId, item, days,
                    subtotals.get(i));
        }
        repository.createPendingDeposit(bookingId, deposit);
        repository.clearCart(cartId);

        return Map.of(
                "bookingId", bookingId,
                "totalAmount", total,
                "depositAmount", deposit,
                "balanceAfterDeposit", total.subtract(deposit),
                "depositPaid", false,
                "paymentMethod", "AT_PICKUP",
                "paymentStatus", "Pending",
                "bookingStatus", "Pending");
    }
}

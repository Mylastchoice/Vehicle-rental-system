package com.group01.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final StripeCheckoutGateway stripe;

    public CheckoutService(CheckoutRepository repository,
            AvailabilityService availabilityService, StripeCheckoutGateway stripe) {
        this.repository = repository;
        this.availabilityService = availabilityService;
        this.stripe = stripe;
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " is required");
        }
    }

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
        return checkout(userId, renterName, renterPhone, licenseNumber,
                pickupLocation, paymentMethod, "DEPOSIT");
    }

    @Transactional
    public Map<String, Object> checkout(Long userId, String renterName,
            String renterPhone, String licenseNumber,
            String pickupLocation, String paymentMethod, String paymentOption) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid user ID");
        }
        requireText(renterName, "Renter name");
        String normalizedPhone = validVietnamesePhone(renterPhone);
        requireText(licenseNumber, "Driving license/ID number");
        requireText(pickupLocation, "Pickup location");
        requireText(paymentMethod, "Payment method");

        String method = paymentMethod.trim().toUpperCase(Locale.ROOT);
        boolean online = "ONLINE".equals(method);
        if (!online && !"AT_PICKUP".equals(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment method must be AT_PICKUP or ONLINE");
        }
        if (online && !stripe.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe sandbox is not configured");
        }

        String option = (paymentOption == null || paymentOption.isBlank())
                ? "DEPOSIT"
                : paymentOption.trim().toUpperCase(Locale.ROOT);
        if (!"DEPOSIT".equals(option) && !"FULL".equals(option)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment option must be DEPOSIT or FULL");
        }
        boolean fullPayment = "FULL".equals(option);

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

        // Mỗi xe có thể có ngày thuê riêng; đơn tổng lấy ngày nhận sớm nhất và ngày trả muộn nhất
        LocalDate pickup = items.get(0).pickupDate();
        LocalDate returned = items.get(0).returnDate();
        long branchId = items.get(0).branchId();

        for (CartLine item : items) {
            if (item.pickupDate().isBefore(pickup)) {
                pickup = item.pickupDate();
            }
            if (item.returnDate().isAfter(returned)) {
                returned = item.returnDate();
            }
            if (branchId != item.branchId()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All vehicles must belong to the same pickup branch");
            }
        }

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

        BigDecimal deposit = fullPayment
                ? total
                : total.multiply(new BigDecimal("0.30"))
                        .setScale(online ? 0 : 2, RoundingMode.HALF_UP);
        if (online && deposit.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Online checkout requires a positive deposit");
        }

        Long bookingId = repository.createBooking(userId, branchId,
                pickupLocation.trim(), pickup, returned,
                renterName.trim(), normalizedPhone,
                licenseNumber.trim(), total, deposit, method);

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
                "paymentOption", option,
                "depositPaid", false,
                "paymentMethod", method,
                "paymentStatus", "Pending",
                "bookingStatus", "Pending");
    }
}
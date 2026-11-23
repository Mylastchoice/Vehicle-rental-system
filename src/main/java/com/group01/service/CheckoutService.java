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
        this.availabilityService = availabilityService;// service để xử lý logic liên quan đến tính khả dụng của xe
        this.stripe = stripe;
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " is required");
        }
    }// kiểm tra xem giá trị chuỗi có hợp lệ hay không, nếu không thì ném ra lỗi 400 Bad Request

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
    }// tính toán tổng phụ của một dòng trong giỏ hàng dựa trên giá thuê hàng ngày, tỷ lệ giảm giá và số ngày thuê, trả về giá trị BigDecimal với 2 chữ số thập phân

    @Transactional // thực hiện quá trình thanh toán và đặt xe cho người dùng, kiểm tra tính hợp lệ của thông tin người dùng, giỏ hàng và xe, tạo đơn đặt xe và chi tiết đơn đặt xe, trả về thông tin đơn đặt xe dưới dạng Map
    public Map<String, Object> checkout(Long userId, String renterName,
            String renterPhone, String licenseNumber,
            String pickupLocation, String paymentMethod) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid user ID");
        }// kiểm tra xem userId có hợp lệ hay không, nếu không thì ném ra lỗi 400 Bad Request
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

        if (!repository.hasValidUserLicense(userId, licenseNumber.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User must be active and have a matching license/ID in profile");
        }// kiểm tra xem người dùng có giấy phép hợp lệ và trùng khớp với thông tin trong hồ sơ hay không, nếu không thì ném ra lỗi 400 Bad Request

        Long cartId = repository.lockCart(userId);
        if (cartId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cart does not exist");
        }// khóa giỏ hàng của người dùng để tránh xung đột khi nhiều người dùng cùng thao tác trên giỏ hàng, nếu giỏ hàng không tồn tại thì ném ra lỗi 400 Bad Request
        List<CartLine> items = repository.getCartLines(cartId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cart is empty");
        }// lấy danh sách các dòng trong giỏ hàng của người dùng, nếu giỏ hàng trống thì ném ra lỗi 400 Bad Request

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
        }// kiểm tra xem tất cả các dòng trong giỏ hàng có cùng ngày thuê, ngày trả và chi nhánh nhận xe hay không, nếu không thì ném ra lỗi 400 Bad Request

        // Khóa từng xe theo mã đã sắp xếp, tránh checkout đồng thời.
        for (CartLine item : items) {
            repository.lockVehicle(item.vehicleCode());
        }// khóa từng xe trong giỏ hàng dựa trên mã xe để tránh xung đột khi nhiều người dùng cùng thao tác trên cùng một xe

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
        }// kiểm tra tính khả dụng của từng xe trong giỏ hàng dựa trên mã xe và ngày thuê/trả, nếu xe không khả dụng thì ném ra lỗi 409 Conflict, tính toán tổng phụ của từng dòng trong giỏ hàng và tổng số tiền thanh toán
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal deposit = total.multiply(new BigDecimal("0.30"))
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
        repository.clearCart(cartId);// tạo chi tiết đơn đặt xe cho từng dòng trong giỏ hàng, tạo bản ghi thanh toán đặt cọc và xóa các mục trong giỏ hàng của người dùng

        return Map.of(
                "bookingId", bookingId,
                "totalAmount", total,
                "depositAmount", deposit,
                "balanceAfterDeposit", total.subtract(deposit),
                "depositPaid", false,
                "paymentMethod", method,
                "paymentStatus", "Pending",
                "bookingStatus", "Pending");
    }
}// service để xử lý logic liên quan đến quá trình thanh toán và đặt xe, bao gồm kiểm tra tính hợp lệ của thông tin người dùng, giỏ hàng và xe, tạo đơn đặt xe và chi tiết đơn đặt xe, tạo bản ghi thanh toán đặt cọc và xóa các mục trong giỏ hàng, trả về thông tin đơn đặt xe dưới dạng Map

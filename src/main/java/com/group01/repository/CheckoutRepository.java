package com.group01.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutRepository {

    private final JdbcTemplate jdbc;

    public CheckoutRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CartLine(
            String vehicleCode,
            LocalDate pickupDate,
            LocalDate returnDate,
            BigDecimal dailyPrice,
            BigDecimal discountRate,
            String vehicleStatus,
            long branchId) {
    }

    public boolean hasValidUserLicense(Long userId, String licenseNumber) {
        String sql = """
            SELECT EXISTS (
                SELECT 1 FROM users
                WHERE user_id = ? AND status = 'ACTIVE'
                  AND license_number IS NOT NULL
                  AND TRIM(license_number) = ?
            )
            """;
        Boolean exists = jdbc.queryForObject(
                sql, Boolean.class, userId, licenseNumber);
        return Boolean.TRUE.equals(exists);
    }

    // Khóa giỏ hàng trong giao dịch checkout.
    public Long lockCart(Long userId) {
        String sql = """
            SELECT cart_id FROM rental_carts
            WHERE user_id = ? FOR UPDATE
            """;
        List<Long> ids = jdbc.query(sql,
                (rs, rowNum) -> rs.getLong("cart_id"), userId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public List<CartLine> getCartLines(Long cartId) {
        String sql = """
            SELECT ci.vehicle_code, ci.pickup_date, ci.return_date,
                   v.daily_price, v.discount_rate,
                   v.status, v.branch_id
            FROM cart_items ci
            JOIN vehicles v ON ci.vehicle_code = v.vehicle_code
            WHERE ci.cart_id = ?
            ORDER BY ci.vehicle_code
            """;
        return jdbc.query(sql, (rs, rowNum) -> new CartLine(
                rs.getString("vehicle_code"),
                rs.getDate("pickup_date").toLocalDate(),
                rs.getDate("return_date").toLocalDate(),
                rs.getBigDecimal("daily_price"),
                rs.getBigDecimal("discount_rate"),
                rs.getString("status"),
                rs.getLong("branch_id")), cartId);
    }

    // PostgreSQL: khóa dòng xe cho tới khi transaction kết thúc.
    public void lockVehicle(String vehicleCode) {
        jdbc.queryForObject(
                "SELECT vehicle_code FROM vehicles WHERE vehicle_code = ? FOR UPDATE",
                String.class, vehicleCode);
    }

    public Long createBooking(Long userId, long branchId,
            String pickupLocation, LocalDate pickupDate, LocalDate returnDate,
            String renterName, String renterPhone, String licenseNumber,
            BigDecimal total, BigDecimal deposit) {
        String sql = """
            INSERT INTO bookings (
                user_id, pickup_branch_id, return_branch_id,
                pickup_location, pickup_date, return_date,
                renter_name, renter_phone, renter_license_number,
                total_amount, deposit_amount, payment_method, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'AT_PICKUP', 'Pending')
            RETURNING booking_id
            """;
        return jdbc.queryForObject(sql, Long.class,
                userId, branchId, branchId, pickupLocation,
                pickupDate, returnDate, renterName, renterPhone,
                licenseNumber, total, deposit);
    }

    public void createBookingDetail(Long bookingId, CartLine line,
            int rentalDays, BigDecimal subtotal) {
        String sql = """
            INSERT INTO booking_details (
                booking_id, vehicle_code, pickup_date, return_date,
                daily_price, discount_rate, rental_days, subtotal
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbc.update(sql, bookingId, line.vehicleCode(),
                line.pickupDate(), line.returnDate(), line.dailyPrice(),
                line.discountRate(), rentalDays, subtotal);
    }

    public void createPendingDeposit(Long bookingId, BigDecimal deposit) {
        String sql = """
            INSERT INTO payments (booking_id, amount, payment_type, status)
            VALUES (?, ?, 'DEPOSIT', 'Pending')
            """;
        jdbc.update(sql, bookingId, deposit);
    }

    public void clearCart(Long cartId) {
        jdbc.update("DELETE FROM cart_items WHERE cart_id = ?", cartId);
    }
}

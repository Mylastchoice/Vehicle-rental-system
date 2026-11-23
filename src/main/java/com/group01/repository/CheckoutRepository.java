package com.group01.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutRepository {// repository để xử lý các truy vấn liên quan đến quá trình thanh toán và đặt xe

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
            long branchId) {// record để lưu thông tin về một dòng trong giỏ hàng, bao gồm mã xe, ngày thuê, ngày trả, giá thuê hàng ngày, tỷ lệ giảm giá, trạng thái xe và ID chi nhánh
    }

    public boolean hasValidUserLicense(Long userId, String licenseNumber) {// kiểm tra xem người dùng có giấy phép hợp lệ hay không dựa trên userId và licenseNumber
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
    }// kiểm tra xem người dùng có giấy phép hợp lệ hay không dựa trên userId và licenseNumber, trả về true nếu hợp lệ, ngược lại trả về false

    // Khóa giỏ hàng trong giao dịch checkout.
    public Long lockCart(Long userId) {
        String sql = """
            SELECT cart_id FROM rental_carts
            WHERE user_id = ? FOR UPDATE
            """;
        List<Long> ids = jdbc.query(sql,
                (rs, rowNum) -> rs.getLong("cart_id"), userId);
        return ids.isEmpty() ? null : ids.get(0);
    }// khóa giỏ hàng của người dùng trong quá trình giao dịch thanh toán dựa trên userId, trả về ID của giỏ hàng nếu tồn tại, ngược lại trả về null

    public List<CartLine> getCartLines(Long cartId) {
        String sql = """
            SELECT ci.vehicle_code, ci.pickup_date, ci.return_date,
                   v.daily_price, v.discount_rate,
                   v.status, v.branch_id
            FROM cart_items ci
            JOIN vehicles v ON ci.vehicle_code = v.vehicle_code
            WHERE ci.cart_id = ?
            ORDER BY ci.vehicle_code
            """;// truy vấn SQL để lấy danh sách các dòng trong giỏ hàng dựa trên cartId, bao gồm mã xe, ngày thuê, ngày trả, giá thuê hàng ngày, tỷ lệ giảm giá, trạng thái xe và ID chi nhánh
        return jdbc.query(sql, (rs, rowNum) -> new CartLine(
                rs.getString("vehicle_code"),
                rs.getDate("pickup_date").toLocalDate(),
                rs.getDate("return_date").toLocalDate(),
                rs.getBigDecimal("daily_price"),
                rs.getBigDecimal("discount_rate"),
                rs.getString("status"),
                rs.getLong("branch_id")), cartId);
    }// thực hiện truy vấn SQL để lấy danh sách các dòng trong giỏ hàng dựa trên cartId và trả về kết quả dưới dạng List<CartLine>

    // PostgreSQL: khóa dòng xe cho tới khi transaction kết thúc.
    public void lockVehicle(String vehicleCode) {
        jdbc.queryForObject(
                "SELECT vehicle_code FROM vehicles WHERE vehicle_code = ? FOR UPDATE",
                String.class, vehicleCode);
    }

    public Long createBooking(Long userId, long branchId,
            String pickupLocation, LocalDate pickupDate, LocalDate returnDate,
            String renterName, String renterPhone, String licenseNumber,
            BigDecimal total, BigDecimal deposit, String paymentMethod) {
        String sql = """
            INSERT INTO bookings (
                user_id, pickup_branch_id, return_branch_id,
                pickup_location, pickup_date, return_date,
                renter_name, renter_phone, renter_license_number,
                total_amount, deposit_amount, payment_method, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending')
            RETURNING booking_id
            """;
        return jdbc.queryForObject(sql, Long.class,
                userId, branchId, branchId, pickupLocation,
                pickupDate, returnDate, renterName, renterPhone,
                licenseNumber, total, deposit, paymentMethod);
    }// thực hiện truy vấn SQL để tạo một bản ghi đặt xe mới trong bảng bookings dựa trên thông tin người dùng, chi nhánh, ngày thuê/trả, thông tin người thuê, tổng số tiền và tiền đặt cọc, trả về ID của bản ghi đặt xe mới tạo

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
    }// thực hiện truy vấn SQL để tạo một bản ghi chi tiết đặt xe mới trong bảng booking_details dựa trên ID đặt xe, thông tin dòng giỏ hàng, số ngày thuê và tổng phụ, không trả về giá trị nào

    public void createPendingDeposit(Long bookingId, BigDecimal deposit) {
        String sql = """
            INSERT INTO payments (booking_id, amount, payment_type, status)
            VALUES (?, ?, 'DEPOSIT', 'Pending')
            """;
        jdbc.update(sql, bookingId, deposit);
    }// thực hiện truy vấn SQL để tạo một bản ghi thanh toán đặt cọc mới trong bảng payments dựa trên ID đặt xe và số tiền đặt cọc, không trả về giá trị nào

    public void clearCart(Long cartId) {
        jdbc.update("DELETE FROM cart_items WHERE cart_id = ?", cartId);
    }
}// repository để xử lý các truy vấn liên quan đến quá trình thanh toán và đặt xe, bao gồm kiểm tra giấy phép hợp lệ, khóa giỏ hàng, lấy danh sách dòng giỏ hàng, khóa xe, tạo bản ghi đặt xe và chi tiết đặt xe, tạo bản ghi thanh toán đặt cọc và xóa các mục trong giỏ hàng.

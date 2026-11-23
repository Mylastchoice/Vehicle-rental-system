package com.group01.repository;

import com.group01.dto.BookingDetailResponse;
import com.group01.dto.BookingPageResponse;
import com.group01.dto.BookingVehicleResponse;
import com.group01.dto.BookingViewResponse;
import com.group01.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** M4 database operations. */
@Repository
public class BookingManagementRepository {
    private final JdbcTemplate jdbc;

    public BookingManagementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT = """
            SELECT booking_id, user_id, renter_name, pickup_date, return_date,
                   pickup_branch_id, return_branch_id, pickup_location,
                   total_amount, deposit_amount, payment_method, status, created_at,
                   requested_return_date, actual_return_date
            FROM bookings
            """;

    private static final RowMapper<BookingViewResponse> BOOKING_MAPPER =
            (rs, row) -> new BookingViewResponse(
                    rs.getLong("booking_id"), rs.getLong("user_id"),
                    rs.getString("renter_name"), rs.getDate("pickup_date").toLocalDate(),
                    rs.getDate("return_date").toLocalDate(),
                    rs.getLong("pickup_branch_id"), rs.getObject("return_branch_id", Long.class),
                    rs.getString("pickup_location"), rs.getBigDecimal("total_amount"),
                    rs.getBigDecimal("deposit_amount"), rs.getString("payment_method"),
                    rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getObject("requested_return_date", LocalDate.class),
                    rs.getObject("actual_return_date", LocalDate.class));

    public BookingPageResponse listForMember(Long userId, int page, int size) {
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE user_id = ?", Long.class, userId);
        List<BookingViewResponse> items = jdbc.query(SELECT + """
                WHERE user_id = ? ORDER BY created_at DESC, booking_id DESC
                LIMIT ? OFFSET ?
                """, BOOKING_MAPPER, userId, size, (long) page * size);
        return pageOf(items, total, page, size);
    }

    public BookingPageResponse listForAdmin(BookingStatus status, LocalDate from,
                                            LocalDate to, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status.getDbValue());
        }
        if (from != null) {
            where.append(" AND pickup_date >= ?");
            args.add(from);
        }
        if (to != null) {
            where.append(" AND pickup_date <= ?");
            args.add(to);
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM bookings" + where,
                Long.class, args.toArray());
        List<Object> listingArgs = new ArrayList<>(args);
        listingArgs.add(size);
        listingArgs.add((long) page * size);
        List<BookingViewResponse> items = jdbc.query(SELECT + where
                + " ORDER BY created_at DESC, booking_id DESC LIMIT ? OFFSET ?",
                BOOKING_MAPPER, listingArgs.toArray());
        return pageOf(items, total, page, size);
    }

    private BookingPageResponse pageOf(List<BookingViewResponse> items, long total,
                                       int page, int size) {
        long pages = (total + size - 1) / size;
        return new BookingPageResponse(items, total, page, size, (int) Math.min(pages, Integer.MAX_VALUE));
    }

    public BookingViewResponse findById(Long id) {
        List<BookingViewResponse> result = jdbc.query(SELECT + " WHERE booking_id = ?",
                BOOKING_MAPPER, id);
        return result.isEmpty() ? null : result.get(0);
    }

    /** Lock booking before checking ownership and changing status. */
    public BookingViewResponse lockById(Long id) {
        List<BookingViewResponse> result = jdbc.query(SELECT + " WHERE booking_id = ? FOR UPDATE",
                BOOKING_MAPPER, id);
        return result.isEmpty() ? null : result.get(0);
    }

    public BookingDetailResponse findDetails(Long id) {
        BookingViewResponse booking = findById(id);
        if (booking == null) return null;
        List<BookingVehicleResponse> vehicles = jdbc.query("""
                SELECT d.vehicle_code, v.name AS vehicle_name,
                       d.pickup_date, d.return_date, d.daily_price,
                       d.discount_rate, d.rental_days, d.subtotal
                FROM booking_details d JOIN vehicles v ON v.vehicle_code = d.vehicle_code
                WHERE d.booking_id = ? ORDER BY d.vehicle_code
                """, (rs, row) -> new BookingVehicleResponse(
                rs.getString("vehicle_code"), rs.getString("vehicle_name"),
                rs.getDate("pickup_date").toLocalDate(),
                rs.getDate("return_date").toLocalDate(),
                rs.getBigDecimal("daily_price"), rs.getBigDecimal("discount_rate"),
                rs.getInt("rental_days"), rs.getBigDecimal("subtotal")), id);
        return new BookingDetailResponse(booking, vehicles);
    }

    public List<String> findVehicleCodes(Long bookingId) {
        return jdbc.query("""
                SELECT vehicle_code FROM booking_details
                WHERE booking_id = ? ORDER BY vehicle_code
                """, (rs, row) -> rs.getString(1), bookingId);
    }

    public void lockVehicle(String vehicleCode) {
        jdbc.queryForObject("SELECT vehicle_code FROM vehicles WHERE vehicle_code = ? FOR UPDATE",
                String.class, vehicleCode);
    }

    public boolean hasConfirmationConflict(Long bookingId, String vehicleCode,
                                           LocalDate pickup, LocalDate returned) {
        Integer overlaps = jdbc.queryForObject("""
                SELECT COUNT(*) FROM booking_details d
                JOIN bookings b ON b.booking_id = d.booking_id
                WHERE d.vehicle_code = ? AND d.booking_id <> ?
                  AND b.status IN ('Pending', 'Confirmed', 'Picked up')
                  AND d.pickup_date < ? AND ? < d.return_date
                """, Integer.class, vehicleCode, bookingId, returned, pickup);
        return overlaps != null && overlaps > 0;
    }

    public void updateStatus(Long bookingId, BookingStatus status) {
        jdbc.update("UPDATE bookings SET status = ? WHERE booking_id = ?",
                status.getDbValue(), bookingId);
    }

    public void saveReturnRequest(Long id, LocalDate requestedDate) {
        jdbc.update("UPDATE bookings SET requested_return_date = ? WHERE booking_id = ?", requestedDate, id);
    }

    public void completeReturn(Long id, LocalDate actualDate) {
        jdbc.update("UPDATE bookings SET status = 'Completed', actual_return_date = ? WHERE booking_id = ?",
                actualDate, id);
    }

    public record Deposit(long id, BigDecimal amount, String status, String sessionId, String checkoutKey) {}

    public Deposit findDeposit(Long bookingId) {
        return jdbc.query("""
                SELECT payment_id, amount, status, transaction_ref, checkout_key
                FROM payments WHERE booking_id = ? AND payment_type = 'DEPOSIT'
                ORDER BY payment_id LIMIT 1
                """, (rs, row) -> new Deposit(rs.getLong("payment_id"), rs.getBigDecimal("amount"),
                rs.getString("status"), rs.getString("transaction_ref"), rs.getString("checkout_key")),
                bookingId).stream().findFirst().orElse(null);
    }

    public Long findBookingForSession(String sessionId) {
        return jdbc.query("SELECT booking_id FROM payments WHERE transaction_ref = ? AND payment_type = 'DEPOSIT'",
                (rs, row) -> rs.getLong("booking_id"), sessionId).stream().findFirst().orElse(null);
    }

    public void saveCheckoutSession(long paymentId, String sessionId) {
        jdbc.update("UPDATE payments SET transaction_ref = ? WHERE payment_id = ?", sessionId, paymentId);
    }

    public void markDepositPaid(long paymentId) {
        jdbc.update("UPDATE payments SET status = 'Paid', paid_at = CURRENT_TIMESTAMP WHERE payment_id = ? AND status = 'Pending'",
                paymentId);
    }

    public void countCompletedRental(String vehicleCode) {
        jdbc.update("UPDATE vehicles SET rental_count = rental_count + 1 WHERE vehicle_code = ?",
                vehicleCode);
    }

    /** A pending refund request is NOT a real money transfer. */
    public void recordPendingRefund(Long bookingId, BigDecimal amount) {
        if (amount.signum() > 0) {
            jdbc.update("""
                    INSERT INTO payments (booking_id, amount, payment_type, status)
                    VALUES (?, ?, 'REFUND', 'Pending')
                    """, bookingId, amount);
        }
    }

    public String findMemberEmail(Long userId) {
        List<String> emails = jdbc.query("SELECT email FROM users WHERE user_id = ?",
                (rs, row) -> rs.getString(1), userId);
        return emails.isEmpty() ? null : emails.get(0);
    }
}

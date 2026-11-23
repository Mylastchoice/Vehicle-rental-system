
package com.group01.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {// repository để xử lý các truy vấn liên quan đến giỏ hàng của người dùng

    private final JdbcTemplate jdbc;// JdbcTemplate để thực hiện các truy vấn SQL

    public CartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Kiem tra nguoi dung ton tai
    public boolean userExists(Long userId) {
        String sql = """
            SELECT EXISTS (
                SELECT 1 FROM users
                WHERE user_id = ?
            )
            """;

        Boolean result = jdbc.queryForObject(
            sql, Boolean.class, userId
        );// thực hiện truy vấn SQL để kiểm tra xem người dùng có tồn tại hay không dựa trên userId

        return Boolean.TRUE.equals(result);// trả về true nếu người dùng tồn tại, ngược lại trả về false
    }

    // Lay gio hang, neu chua co thi tao moi
    public Long getOrCreateCart(Long userId) {

        String insertSql = """
            INSERT INTO rental_carts (user_id)
            VALUES (?)
            ON CONFLICT (user_id) DO NOTHING
            """;// truy vấn SQL để tạo mới giỏ hàng cho người dùng nếu chưa có, nếu đã có thì không làm gì cả

        jdbc.update(insertSql, userId);

        String selectSql = """
            SELECT cart_id
            FROM rental_carts
            WHERE user_id = ?
            """;

        return jdbc.queryForObject(
            selectSql, Long.class, userId
        );// thực hiện truy vấn SQL để lấy ID của giỏ hàng và trả về kết quả
    }

    // Them xe, neu xe da co thi cap nhat ngay thue
    public void saveItem(
            Long cartId,
            String vehicleCode,
            LocalDate pickupDate,
            LocalDate returnDate) {// thêm xe vào giỏ hàng, nếu xe đã có trong giỏ hàng thì cập nhật ngày thuê và ngày trả xe

        String sql = """
            INSERT INTO cart_items
                (cart_id, vehicle_code,
                 pickup_date, return_date)
            VALUES (?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_cart_vehicle
            DO UPDATE SET
                pickup_date = EXCLUDED.pickup_date,
                return_date = EXCLUDED.return_date
            """;// truy vấn SQL để thêm xe vào giỏ hàng, nếu xe đã có trong giỏ hàng thì cập nhật ngày thuê và ngày trả xe

        jdbc.update(
            sql,
            cartId,
            vehicleCode,
            pickupDate,
            returnDate
        );// thực hiện truy vấn SQL để thêm xe vào giỏ hàng hoặc cập nhật ngày thuê và ngày trả xe nếu xe đã có trong giỏ hàng
    }

    // Xem danh sach xe trong gio
    public List<Map<String, Object>> getItems(Long userId) {
// truy vấn SQL để lấy danh sách các xe trong giỏ hàng của người dùng dựa trên userId
        String sql = """
            SELECT
                ci.cart_item_id AS "cartItemId",
                ci.vehicle_code AS "vehicleCode",
                v.name AS "vehicleName",
                v.branch_id AS "branchId",
                b.branch_name AS "branchName",
                ci.pickup_date::text AS "pickupDate",
                ci.return_date::text AS "returnDate",
                v.daily_price AS "dailyPrice",
                (ci.return_date - ci.pickup_date)
                    AS "rentalDays",
                (
                    v.daily_price
                    * (1 - v.discount_rate)
                    * (ci.return_date - ci.pickup_date)
                ) AS "estimatedSubtotal"
            FROM cart_items ci
            JOIN rental_carts c
                ON ci.cart_id = c.cart_id
            JOIN vehicles v
                ON ci.vehicle_code = v.vehicle_code
            JOIN branches b ON b.branch_id = v.branch_id
            WHERE c.user_id = ?
            ORDER BY ci.cart_item_id
            """;

        return jdbc.queryForList(sql, userId);
    }// thực hiện truy vấn SQL để lấy danh sách các xe trong giỏ hàng của người dùng và trả về kết quả dưới dạng List<Map<String, Object>>

    // Only change an item that belongs to the authenticated member's cart.
    public int updateDates(Long userId, Long itemId, LocalDate pickupDate, LocalDate returnDate) {
        return jdbc.update("""
                UPDATE cart_items ci SET pickup_date = ?, return_date = ?
                FROM rental_carts c
                WHERE c.cart_id = ci.cart_id AND c.user_id = ? AND ci.cart_item_id = ?
                """, pickupDate, returnDate, userId, itemId);
    }// thực hiện truy vấn SQL để cập nhật ngày thuê và ngày trả xe cho một mục trong giỏ hàng của người dùng dựa trên userId và itemId

    public String findVehicleCodeForItem(Long userId, Long itemId) {
        List<String> codes = jdbc.query("""
                SELECT ci.vehicle_code FROM cart_items ci
                JOIN rental_carts c ON c.cart_id = ci.cart_id
                WHERE c.user_id = ? AND ci.cart_item_id = ?
                """, (rs, rowNum) -> rs.getString("vehicle_code"), userId, itemId);
        return codes.isEmpty() ? null : codes.get(0);
    }// thực hiện truy vấn SQL để tìm mã xe cho một mục trong giỏ hàng của người dùng dựa trên userId và itemId, nếu không tìm thấy thì trả về null

    // Xoa xe khoi gio cua dung nguoi dung
    public int deleteItem(Long userId, Long itemId) {

        String sql = """
            DELETE FROM cart_items ci
            USING rental_carts c
            WHERE ci.cart_id = c.cart_id
              AND c.user_id = ?
              AND ci.cart_item_id = ?
            """;

        return jdbc.update(sql, userId, itemId);// thực hiện truy vấn SQL để xóa một mục trong giỏ hàng của người dùng dựa trên userId và itemId, trả về số lượng bản ghi bị ảnh hưởng
    }
}


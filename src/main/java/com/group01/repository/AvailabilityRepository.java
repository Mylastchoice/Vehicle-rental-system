
package com.group01.repository;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository// repository để xử lý các truy vấn liên quan đến tính khả dụng của xe
public class AvailabilityRepository {

    private final JdbcTemplate jdbc;// JdbcTemplate để thực hiện các truy vấn SQL

    public AvailabilityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;// constructor để khởi tạo AvailabilityRepository với JdbcTemplate
    }

    public boolean isAvailable(
            String vehicleCode,
            LocalDate pickupDate,
            LocalDate returnDate) {// kiểm tra tính khả dụng của xe dựa trên mã xe và ngày thuê/trả

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM vehicles v
                WHERE v.vehicle_code = ?
                  AND v.status = 'Available'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM booking_details bd
                      JOIN bookings b
                        ON bd.booking_id = b.booking_id
                      WHERE bd.vehicle_code = v.vehicle_code
                        AND b.status IN (
                            'Pending',
                            'Confirmed',
                            'Picked up'
                        )
                        AND bd.pickup_date < ?
                        AND bd.return_date > ?
                  )
            )
            """;// truy vấn SQL để kiểm tra xem xe có khả dụng hay không dựa trên mã xe và ngày thuê/trả

        Boolean result = jdbc.queryForObject(
            sql,
            Boolean.class,
            vehicleCode,
            returnDate,
            pickupDate
        );// thực hiện truy vấn SQL và trả về kết quả là một Boolean

        return Boolean.TRUE.equals(result);
    }
}


package com.group01.repository;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AvailabilityRepository {

    private final JdbcTemplate jdbc;

    public AvailabilityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isAvailable(
            String vehicleCode,
            LocalDate pickupDate,
            LocalDate returnDate) {

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
            """;

        Boolean result = jdbc.queryForObject(
            sql,
            Boolean.class,
            vehicleCode,
            returnDate,
            pickupDate
        );

        return Boolean.TRUE.equals(result);
    }
}

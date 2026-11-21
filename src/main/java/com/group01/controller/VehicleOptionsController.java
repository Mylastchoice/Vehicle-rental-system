package com.group01.controller;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight read-only picker for integrating catalog browsing until M5 catalog is merged. */
@RestController
@RequestMapping("/api")
public class VehicleOptionsController {
    private final JdbcTemplate jdbc;

    public VehicleOptionsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/vehicle-options")
    public List<Map<String, Object>> vehicleOptions() {
        return jdbc.queryForList("""
            SELECT v.vehicle_code AS "vehicleCode", v.name AS "name",
                   v.daily_price AS "dailyPrice", v.discount_rate AS "discountRate",
                   v.image_url AS "imageUrl", v.status AS "status",
                   v.branch_id AS "branchId", b.branch_name AS "branchName",
                   v.category_id AS "categoryId", c.category_name AS "categoryName",
                   v.short_description AS "shortDescription",
                   v.detailed_description AS "detailedDescription",
                   v.license_plate AS "licensePlate"
            FROM vehicles v
            JOIN branches b ON b.branch_id = v.branch_id
            JOIN categories c ON c.category_id = v.category_id
            ORDER BY v.name, v.vehicle_code LIMIT 80
            """);
    }
}


package com.group01.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseRepository {

    private final JdbcTemplate jdbc;

    public DatabaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String getDatabaseName() {
        return jdbc.queryForObject(
            "SELECT current_database()",
            String.class
        );
    }
}

package com.group01.support;

import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class PostgresAvailableCondition implements ExecutionCondition {

    static String url() {
        return env("TEST_DB_URL", "jdbc:postgresql://localhost:5432/vehicle_rental_test");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            DriverManager.setLoginTimeout(3);
            try (Connection ignored = DriverManager.getConnection(url(), env("TEST_DB_USERNAME", "postgres"),
                    System.getenv().getOrDefault("TEST_DB_PASSWORD", ""))) {
                return ConditionEvaluationResult.enabled("PostgreSQL reachable");
            }
        } catch (Exception e) {
            if ("true".equalsIgnoreCase(System.getenv("REQUIRE_DB"))) {
                throw new IllegalStateException("REQUIRE_DB=true but PostgreSQL is not reachable at " + url(), e);
            }
            return ConditionEvaluationResult.disabled("PostgreSQL not reachable at " + url() + " - test skipped");
        }
    }
}

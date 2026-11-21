package com.group01.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks tests that need a running PostgreSQL (TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD, defaults in
 * application-test.properties). Without a database they are skipped, unless REQUIRE_DB=true (used by CI) -
 * then they fail so that a missing database can never hide broken tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(PostgresAvailableCondition.class)
public @interface RequiresPostgres {
}

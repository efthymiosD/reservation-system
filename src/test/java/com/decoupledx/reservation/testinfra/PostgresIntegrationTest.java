package com.decoupledx.reservation.testinfra;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "app.clock.fixed-instant=2026-09-01T10:00:00Z",
        "app.security.issuer-uri="
})
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbc;

    // All integration test classes share one Postgres container and dataset; every
    // class must start from a clean booking state regardless of execution order.
    @BeforeEach
    void cleanBookingTables() {
        jdbc.update("TRUNCATE resource_blocks, reservations");
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}

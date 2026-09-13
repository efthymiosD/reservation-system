package com.decoupledx.reservation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

class DatabaseConstraintsIntegrationTest extends PostgresIntegrationTest {

    private static final UUID FIELD_1 = UUID.fromString("a0000000-0000-0000-0000-000000000101");
    private static final UUID FIELD_2 = UUID.fromString("a0000000-0000-0000-0000-000000000102");
    private static final UUID TEST_CUSTOMER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DAY = "2026-09-10";

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.update("TRUNCATE recurring_reservations, reservations");
    }

    @Test
    void rejectsOverlappingActiveReservationsForSameResource() {
        insertReservation(FIELD_1, "cust-a", at(18, 0), at(19, 0), "ACTIVE");
        assertThatThrownBy(() -> insertReservation(FIELD_1, "cust-b", at(18, 30), at(19, 30), "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reservations_no_resource_overlap");
    }

    @Test
    void allowsBackToBackReservationsOnSameResource() {
        insertReservation(FIELD_1, "cust-a", at(16, 0), at(17, 0), "ACTIVE");
        assertThatCode(() -> insertReservation(FIELD_1, "cust-b", at(17, 0), at(18, 0), "ACTIVE"))
                .doesNotThrowAnyException();
    }

    @Test
    void cancelledReservationsDoNotParticipateInResourceConstraint() {
        insertReservation(FIELD_1, "cust-a", at(20, 0), at(21, 0), "ACTIVE");
        assertThatCode(() -> insertReservation(FIELD_1, "cust-b", at(20, 30), at(21, 30), "CANCELLED"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsOverlappingActiveReservationsForSameCustomer() {
        insertReservation(FIELD_1, "cust-x", at(18, 0), at(19, 0), "ACTIVE");
        assertThatThrownBy(() -> insertReservation(FIELD_2, "cust-x", at(18, 30), at(19, 30), "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reservations_no_customer_overlap");
    }

    @Test
    void allowsSameCustomerWithNonOverlappingReservations() {
        insertReservation(FIELD_1, "cust-y", at(18, 0), at(19, 0), "ACTIVE");
        assertThatCode(() -> insertReservation(FIELD_2, "cust-y", at(19, 0), at(20, 0), "ACTIVE"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRecurringReservationPeriodWithEndNotAfterStart() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO recurring_reservations
                    (id, resource_id, customer_id, weekday, start_time, end_time, status,
                     next_occurrence, window_months, created_at, version)
                VALUES (?, ?, ?, 'MONDAY', '18:00', '18:00', 'ACTIVE', '2026-09-10', 1, now(), 0)
                """, UUID.randomUUID(), FIELD_1, TEST_CUSTOMER))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("recurring_reservation_period_check");
    }

    @Test
    void rejectsRecurringReservationWithUnsupportedWeekday() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO recurring_reservations
                    (id, resource_id, customer_id, weekday, start_time, end_time, status,
                     next_occurrence, window_months, created_at, version)
                VALUES (?, ?, ?, 'FRIDAYZ', '18:00', '19:00', 'ACTIVE', '2026-09-10', 1, now(), 0)
                """, UUID.randomUUID(), FIELD_1, TEST_CUSTOMER))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("recurring_reservation_day_check");
    }

    private void insertReservation(UUID resourceId, String customerId, OffsetDateTime start,
                                   OffsetDateTime end, String status) {
        jdbc.update("""
                INSERT INTO reservations
                    (id, resource_id, customer_id, start_time, end_time, status,
                     price_amount, price_currency, created_at, version)
                VALUES (?, ?, ?, ?, ?, ?, 80.00, 'PLN', now(), 0)
                """,
                UUID.randomUUID(), resourceId, customerId, start, end, status);
    }

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.parse(DAY + "T00:00:00Z")
                .withHour(hour)
                .withMinute(minute)
                .withOffsetSameInstant(ZoneOffset.UTC);
    }
}

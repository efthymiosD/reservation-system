package com.decoupledx.reservation.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.Money;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.reservation.domain.model.Reservation;

class ReservationTest {

    private static final Instant START = Instant.parse("2026-09-01T16:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-29T10:00:00Z");
    private static final CancellationPolicy TWO_HOURS = new CancellationPolicy(Duration.ofHours(2));

    private Reservation newReservation() {
        return Reservation.create(
                ResourceId.random(),
                CustomerId.of("customer-1"),
                ReservationPeriod.ofStartAndDuration(START, Duration.ofMinutes(90)),
                Money.of(new BigDecimal("120"), Currency.getInstance("PLN")),
                NOW);
    }

    @Test
    void createsActiveReservationWithSnapshottedPrice() {
        Reservation reservation = newReservation();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getCreatedAt()).isEqualTo(NOW);
        assertThat(reservation.getCancelledAt()).isNull();
        assertThat(reservation.getPrice().amount()).isEqualByComparingTo("120.00");
    }

    @Test
    void cancelsWhenCurrentPolicyAllows() {
        Reservation reservation = newReservation();
        reservation.cancel(Instant.parse("2026-09-01T13:00:00Z"), TWO_HOURS);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledAt()).isEqualTo(Instant.parse("2026-09-01T13:00:00Z"));
    }

    @Test
    void rejectsCancellationAfterCurrentPolicyDeadline() {
        Reservation reservation = newReservation();
        assertThatThrownBy(() -> reservation.cancel(Instant.parse("2026-09-01T15:00:00Z"), TWO_HOURS))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.CANCELLATION_DEADLINE_PASSED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void cancellationIsEvaluatedAgainstPolicyPassedAtCancelTime() {
        Reservation reservation = newReservation();
        CancellationPolicy fourHours = new CancellationPolicy(Duration.ofHours(4));
        assertThatThrownBy(() -> reservation.cancel(Instant.parse("2026-09-01T13:00:00Z"), fourHours))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsCancellingAlreadyCancelledReservation() {
        Reservation reservation = newReservation();
        reservation.cancel(Instant.parse("2026-09-01T13:00:00Z"), TWO_HOURS);
        assertThatThrownBy(() -> reservation.cancel(Instant.parse("2026-09-01T13:00:00Z"), TWO_HOURS))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.RESERVATION_ALREADY_CANCELLED);
    }

    @Test
    void administrativeCancellationIgnoresPolicyDeadline() {
        Reservation reservation = newReservation();
        reservation.cancelAdministratively(Instant.parse("2026-09-01T15:59:00Z"), CustomerId.of("admin-actor"));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void rejectsAdministrativeCancellationOfElapsedReservation() {
        Reservation reservation = newReservation();  // ends START + 90m = 17:30Z
        java.time.Instant afterEnd = Instant.parse("2026-09-01T17:31:00Z");

        assertThatThrownBy(() -> reservation.cancelAdministratively(afterEnd,
                CustomerId.of("admin-actor")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer be cancelled");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void rejectsAdministrativeCancellationOfCancelledReservation() {
        Reservation reservation = newReservation();
        reservation.cancelAdministratively(NOW, CustomerId.of("admin-actor"));
        assertThatThrownBy(() -> reservation.cancelAdministratively(NOW, CustomerId.of("admin-actor")))
                .isInstanceOf(BusinessException.class);
    }
}

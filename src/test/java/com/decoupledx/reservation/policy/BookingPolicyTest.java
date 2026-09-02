package com.decoupledx.reservation.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.policy.domain.model.BookingPolicy;

class BookingPolicyTest {

    private static final ZoneId UTC = ZoneOffset.UTC;

    private static final BookingPolicy POLICY = new BookingPolicy(
            Duration.ofHours(1),
            Duration.ofHours(2),
            Duration.ofMinutes(30),
            Duration.ofMinutes(30),
            Period.ofMonths(1));

    @Test
    void acceptsDurationsWithinBoundsOnStepGrid() {
        assertThatCode(() -> POLICY.validateDuration(Duration.ofMinutes(60))).doesNotThrowAnyException();
        assertThatCode(() -> POLICY.validateDuration(Duration.ofMinutes(90))).doesNotThrowAnyException();
        assertThatCode(() -> POLICY.validateDuration(Duration.ofMinutes(120))).doesNotThrowAnyException();
    }

    @Test
    void rejectsDurationsOutsideBoundsOrOffGrid() {
        assertThatThrownBy(() -> POLICY.validateDuration(Duration.ofMinutes(45)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_RESERVATION_DURATION);
        assertThatThrownBy(() -> POLICY.validateDuration(Duration.ofMinutes(150)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> POLICY.validateDuration(Duration.ofMinutes(75)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void acceptsStartTimeOnIncrementRelativeToOpeningTime() {
        LocalTime opensAt = LocalTime.of(14, 0);
        assertThatCode(() -> POLICY.validateStartTime(at("2026-09-01T14:00:00Z"), opensAt))
                .doesNotThrowAnyException();
        assertThatCode(() -> POLICY.validateStartTime(at("2026-09-01T15:30:00Z"), opensAt))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsStartTimeOffIncrementRelativeToOpeningTime() {
        LocalTime opensAt = LocalTime.of(14, 0);
        assertThatThrownBy(() -> POLICY.validateStartTime(at("2026-09-01T14:15:00Z"), opensAt))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_START_TIME);
        assertThatThrownBy(() -> POLICY.validateStartTime(at("2026-09-01T13:30:00Z"), opensAt))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void appliesIncrementRelativeToNonRoundedOpeningTime() {
        LocalTime opensAt = LocalTime.of(14, 15);
        assertThatCode(() -> POLICY.validateStartTime(at("2026-09-01T14:45:00Z"), opensAt))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> POLICY.validateStartTime(at("2026-09-01T15:00:00Z"), opensAt))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void acceptsReservationsWithinAdvanceBookingWindow() {
        Instant now = Instant.parse("2026-08-29T12:00:00Z");
        assertThatCode(() -> POLICY.validateAdvanceBooking(now, at("2026-09-20T14:00:00Z").toInstant(), UTC))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsReservationsBeyondAdvanceBookingWindow() {
        Instant now = Instant.parse("2026-08-29T12:00:00Z");
        assertThatThrownBy(() -> POLICY.validateAdvanceBooking(now, at("2026-10-05T14:00:00Z").toInstant(), UTC))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.ADVANCE_BOOKING_LIMIT_EXCEEDED);
    }

    @Test
    void rejectsInvalidPolicyConfiguration() {
        assertThatThrownBy(() -> new BookingPolicy(
                Duration.ofHours(2), Duration.ofHours(1),
                Duration.ofMinutes(30), Duration.ofMinutes(30), Period.ofMonths(1)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new BookingPolicy(
                Duration.ZERO, Duration.ofHours(1),
                Duration.ofMinutes(30), Duration.ofMinutes(30), Period.ofMonths(1)))
                .isInstanceOf(BusinessException.class);
    }

    private static ZonedDateTime at(String instant) {
        return Instant.parse(instant).atZone(Clock.systemUTC().getZone());
    }
}

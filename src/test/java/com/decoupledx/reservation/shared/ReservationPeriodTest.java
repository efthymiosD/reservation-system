package com.decoupledx.reservation.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

class ReservationPeriodTest {

    private static final Instant AT_14_00 = Instant.parse("2026-09-01T12:00:00Z");
    private static final Instant AT_15_00 = Instant.parse("2026-09-01T13:00:00Z");
    private static final Instant AT_15_30 = Instant.parse("2026-09-01T13:30:00Z");
    private static final Instant AT_16_00 = Instant.parse("2026-09-01T14:00:00Z");

    @Test
    void rejectsPeriodWhereStartIsNotBeforeEnd() {
        assertThatThrownBy(() -> ReservationPeriod.of(AT_15_00, AT_15_00))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_RESERVATION_PERIOD);
        assertThatThrownBy(() -> ReservationPeriod.of(AT_16_00, AT_15_00))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void computesDuration() {
        ReservationPeriod period = ReservationPeriod.of(AT_14_00, AT_15_30);
        assertThat(period.duration()).isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    void buildsFromStartAndDuration() {
        ReservationPeriod period = ReservationPeriod.ofStartAndDuration(AT_14_00, Duration.ofMinutes(90));
        assertThat(period.start()).isEqualTo(AT_14_00);
        assertThat(period.end()).isEqualTo(AT_15_30);
    }

    @Test
    void backToBackPeriodsDoNotOverlap() {
        ReservationPeriod first = ReservationPeriod.of(AT_14_00, AT_15_00);
        ReservationPeriod second = ReservationPeriod.of(AT_15_00, AT_16_00);
        assertThat(first.overlaps(second)).isFalse();
        assertThat(second.overlaps(first)).isFalse();
    }

    @Test
    void overlappingPeriodsAreDetected() {
        ReservationPeriod first = ReservationPeriod.of(AT_14_00, AT_15_30);
        ReservationPeriod second = ReservationPeriod.of(AT_15_00, AT_16_00);
        assertThat(first.overlaps(second)).isTrue();
        assertThat(second.overlaps(first)).isTrue();
    }

    @Test
    void containedPeriodOverlaps() {
        ReservationPeriod outer = ReservationPeriod.of(AT_14_00, AT_16_00);
        ReservationPeriod inner = ReservationPeriod.of(AT_15_00, AT_15_30);
        assertThat(outer.overlaps(inner)).isTrue();
    }

    @Test
    void containsInstantWithHalfOpenSemantics() {
        ReservationPeriod period = ReservationPeriod.of(AT_14_00, AT_15_00);
        assertThat(period.contains(AT_14_00)).isTrue();
        assertThat(period.contains(AT_15_30)).isFalse();
        assertThat(period.contains(AT_15_00)).isFalse();
    }
}

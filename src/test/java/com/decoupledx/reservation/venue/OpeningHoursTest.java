package com.decoupledx.reservation.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.venue.domain.model.DailyOpeningHours;
import com.decoupledx.reservation.venue.domain.model.OpeningHours;

class OpeningHoursTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private static final OpeningHours HOURS = new OpeningHours(Map.of(
            DayOfWeek.MONDAY, new DailyOpeningHours(LocalTime.of(14, 0), LocalTime.of(23, 0)),
            DayOfWeek.TUESDAY, new DailyOpeningHours(LocalTime.of(14, 15), LocalTime.of(22, 15))));

    private static Instant warsaw(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(WARSAW).toInstant();
    }

    @Test
    void acceptsPeriodFittingInsideOpeningInterval() {
        ReservationPeriod period = ReservationPeriod.of(warsaw("2026-08-31T18:00"), warsaw("2026-08-31T19:30"));
        assertThat(HOURS.fits(period, WARSAW)).isTrue();
    }

    @Test
    void acceptsPeriodEndingExactlyAtClosingTime() {
        ReservationPeriod period = ReservationPeriod.of(warsaw("2026-08-31T22:00"), warsaw("2026-08-31T23:00"));
        assertThat(HOURS.fits(period, WARSAW)).isTrue();
    }

    @Test
    void acceptsPeriodStartingExactlyAtOpeningTime() {
        ReservationPeriod period = ReservationPeriod.of(warsaw("2026-08-31T14:00"), warsaw("2026-08-31T15:00"));
        assertThat(HOURS.fits(period, WARSAW)).isTrue();
    }

    @Test
    void rejectsPeriodEndingAfterClosingTime() {
        ReservationPeriod period = ReservationPeriod.of(warsaw("2026-08-31T22:30"), warsaw("2026-08-31T23:30"));
        assertThat(HOURS.fits(period, WARSAW)).isFalse();
    }

    @Test
    void rejectsPeriodStartingBeforeOpeningTime() {
        ReservationPeriod period = ReservationPeriod.of(warsaw("2026-08-31T13:30"), warsaw("2026-08-31T15:00"));
        assertThat(HOURS.fits(period, WARSAW)).isFalse();
    }

    @Test
    void rejectsPeriodOnClosedDay() {
        ReservationPeriod period = ReservationPeriod.of(warsaw("2026-09-02T18:00"), warsaw("2026-09-02T19:00"));
        assertThat(HOURS.fits(period, WARSAW)).isFalse();
    }

    @Test
    void rejectsPeriodCrossingMidnight() {
        OpeningHours lateHours = new OpeningHours(Map.of(
                DayOfWeek.MONDAY, new DailyOpeningHours(LocalTime.of(18, 0), LocalTime.of(23, 59))));
        ReservationPeriod period = ReservationPeriod.of(warsaw("2026-08-31T23:00"), warsaw("2026-09-01T00:30"));
        assertThat(lateHours.fits(period, WARSAW)).isFalse();
    }

    @Test
    void rejectsDailyHoursClosingBeforeOpening() {
        assertThatThrownBy(() -> new DailyOpeningHours(LocalTime.of(23, 0), LocalTime.of(14, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_OPENING_HOURS);
    }

    @Test
    void rejectsZeroLengthInterval() {
        assertThatThrownBy(() -> new DailyOpeningHours(LocalTime.of(14, 0), LocalTime.of(14, 0)))
                .isInstanceOf(BusinessException.class);
    }
}

package com.decoupledx.reservation.venue.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.decoupledx.reservation.shared.domain.ReservationPeriod;

public record OpeningHours(Map<DayOfWeek, DailyOpeningHours> perDay) {

    public OpeningHours {
        Objects.requireNonNull(perDay, "perDay must not be null");
        perDay = Map.copyOf(perDay);
    }

    public Optional<DailyOpeningHours> on(DayOfWeek day) {
        return Optional.ofNullable(perDay.get(day));
    }

    public Optional<LocalTime> opensAt(DayOfWeek day) {
        return on(day).map(DailyOpeningHours::opensAt);
    }

    public boolean fits(ReservationPeriod period, ZoneId zone) {
        LocalDate startDate = period.start().atZone(zone).toLocalDate();
        LocalDateTime start = period.start().atZone(zone).toLocalDateTime();
        LocalDateTime end = period.end().atZone(zone).toLocalDateTime();
        if (on(startDate.getDayOfWeek())
                .map(hours -> hours.contains(startDate, start, end)).orElse(false)) {
            return true;
        }
        LocalDate previousDay = startDate.minusDays(1);
        return on(previousDay.getDayOfWeek())
                .map(hours -> hours.contains(previousDay, start, end)).orElse(false);
    }
}

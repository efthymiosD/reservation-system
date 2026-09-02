package com.decoupledx.reservation.venue.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
        ZonedDateTime start = period.start().atZone(zone);
        ZonedDateTime end = period.end().atZone(zone);
        LocalDate startDate = start.toLocalDate();
        if (!startDate.equals(end.toLocalDate())) {
            return false;
        }
        return on(start.getDayOfWeek())
                .map(hours -> hours.covers(start.toLocalDateTime(), end.toLocalDateTime()))
                .orElse(false);
    }
}

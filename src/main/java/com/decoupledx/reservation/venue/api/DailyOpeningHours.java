package com.decoupledx.reservation.venue.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;

/**
 * One venue day's opening window. When {@code closesAt} is before {@code opensAt}
 * the window crosses midnight and ends on the following calendar day (e.g.
 * 14:00–00:00 means open until midnight, 20:00–02:00 means open until 02:00 the
 * next day). Equal times are rejected.
 */
public record DailyOpeningHours(LocalTime opensAt, LocalTime closesAt) {

    public DailyOpeningHours {
        Objects.requireNonNull(opensAt, "opensAt must not be null");
        Objects.requireNonNull(closesAt, "closesAt must not be null");
        if (opensAt.equals(closesAt)) {
            throw new BusinessException(ErrorCode.INVALID_OPENING_HOURS);
        }
    }

    /** True when the window crosses midnight (close time before open time). */
    public boolean overnight() {
        return closesAt.isBefore(opensAt);
    }

    /**
     * Where the window anchored on {@code day} ends: same day when not overnight,
     * the following calendar day otherwise.
     */
    public LocalDateTime windowEnd(LocalDate day) {
        return overnight() ? day.plusDays(1).atTime(closesAt) : day.atTime(closesAt);
    }

    /** Where the window anchored on {@code day} starts. */
    public LocalDateTime windowStart(LocalDate day) {
        return day.atTime(opensAt);
    }

    /** True when the period is fully inside this window anchored on {@code day}. */
    public boolean contains(LocalDate day, LocalDateTime start, LocalDateTime end) {
        return !start.isBefore(windowStart(day)) && !end.isAfter(windowEnd(day));
    }
}
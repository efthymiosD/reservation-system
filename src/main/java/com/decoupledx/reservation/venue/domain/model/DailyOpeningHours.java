package com.decoupledx.reservation.venue.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;

public record DailyOpeningHours(LocalTime opensAt, LocalTime closesAt) {

    public DailyOpeningHours {
        Objects.requireNonNull(opensAt, "opensAt must not be null");
        Objects.requireNonNull(closesAt, "closesAt must not be null");
        if (!closesAt.isAfter(opensAt)) {
            throw new BusinessException(ErrorCode.INVALID_OPENING_HOURS);
        }
    }

    public boolean covers(LocalDateTime start, LocalDateTime end) {
        return !start.toLocalTime().isBefore(opensAt) && !end.toLocalTime().isAfter(closesAt);
    }
}

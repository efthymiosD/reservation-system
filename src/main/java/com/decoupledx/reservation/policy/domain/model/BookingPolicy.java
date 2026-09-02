package com.decoupledx.reservation.policy.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;

public record BookingPolicy(
        Duration minDuration,
        Duration maxDuration,
        Duration durationStep,
        Duration startTimeStep,
        Period maxAdvanceBooking) {

    public BookingPolicy {
        Objects.requireNonNull(minDuration, "minDuration must not be null");
        Objects.requireNonNull(maxDuration, "maxDuration must not be null");
        Objects.requireNonNull(durationStep, "durationStep must not be null");
        Objects.requireNonNull(startTimeStep, "startTimeStep must not be null");
        Objects.requireNonNull(maxAdvanceBooking, "maxAdvanceBooking must not be null");
        if (minDuration.isNegative() || minDuration.isZero()) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_POLICY, "Minimum duration must be positive");
        }
        if (maxDuration.compareTo(minDuration) < 0) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_POLICY,
                    "Maximum duration must not be smaller than minimum duration");
        }
        if (durationStep.isNegative() || durationStep.isZero()) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_POLICY, "Duration step must be positive");
        }
        if (startTimeStep.isNegative() || startTimeStep.isZero()) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_POLICY, "Start time step must be positive");
        }
        if (maxAdvanceBooking.isNegative() || maxAdvanceBooking.isZero()) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_POLICY, "Advance booking window must be positive");
        }
    }

    public void validateDuration(Duration duration) {
        if (duration == null
                || duration.compareTo(minDuration) < 0
                || duration.compareTo(maxDuration) > 0
                || duration.toMinutes() % durationStep.toMinutes() != 0) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_DURATION,
                    "Duration must be between %s and %s in %s increments"
                            .formatted(minDuration, maxDuration, durationStep));
        }
    }

    public void validateStartTime(ZonedDateTime start, LocalTime opensAt) {
        long minutesFromOpening = Duration.between(opensAt, start.toLocalTime()).toMinutes();
        if (minutesFromOpening < 0 || minutesFromOpening % startTimeStep.toMinutes() != 0) {
            throw new BusinessException(ErrorCode.INVALID_START_TIME,
                    "Start time must be on a %s increment relative to opening time"
                            .formatted(startTimeStep));
        }
    }

    public void validateAdvanceBooking(Instant now, Instant start, ZoneId zone) {
        LocalDate latestBookableDate = now.atZone(zone).toLocalDate().plus(maxAdvanceBooking);
        if (start.atZone(zone).toLocalDate().isAfter(latestBookableDate)) {
            throw new BusinessException(ErrorCode.ADVANCE_BOOKING_LIMIT_EXCEEDED,
                    "Reservations can be made at most %s in advance".formatted(maxAdvanceBooking));
        }
    }
}

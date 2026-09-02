package com.decoupledx.reservation.shared.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record ReservationPeriod(Instant start, Instant end) {

    public ReservationPeriod {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_PERIOD,
                    "Period start must be before period end");
        }
    }

    public static ReservationPeriod of(Instant start, Instant end) {
        return new ReservationPeriod(start, end);
    }

    public static ReservationPeriod ofStartAndDuration(Instant start, Duration duration) {
        return new ReservationPeriod(start, start.plus(duration));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public boolean overlaps(ReservationPeriod other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(start) && instant.isBefore(end);
    }
}

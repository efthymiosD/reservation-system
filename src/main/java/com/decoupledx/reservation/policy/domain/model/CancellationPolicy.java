package com.decoupledx.reservation.policy.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;

public record CancellationPolicy(Duration deadlineBeforeStart) {

    public CancellationPolicy {
        Objects.requireNonNull(deadlineBeforeStart, "deadlineBeforeStart must not be null");
        if (deadlineBeforeStart.isNegative()) {
            throw new BusinessException(ErrorCode.INVALID_CANCELLATION_POLICY,
                    "Cancellation deadline must not be negative");
        }
    }

    public boolean allowsCancellation(Instant now, Instant reservationStart) {
        return !now.plus(deadlineBeforeStart).isAfter(reservationStart);
    }
}

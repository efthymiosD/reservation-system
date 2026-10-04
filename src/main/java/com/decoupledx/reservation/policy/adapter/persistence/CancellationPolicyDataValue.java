package com.decoupledx.reservation.policy.adapter.persistence;

import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import java.time.Duration;

/**
 * Persistence view of a cancellation policy.
 */
public record CancellationPolicyDataValue(long deadlineBeforeStartMinutes) {

    public static CancellationPolicyDataValue from(CancellationPolicy policy) {
        return new CancellationPolicyDataValue(policy.deadlineBeforeStart().toMinutes());
    }

    public CancellationPolicy toCancellationPolicy() {
        return new CancellationPolicy(Duration.ofMinutes(deadlineBeforeStartMinutes));
    }
}

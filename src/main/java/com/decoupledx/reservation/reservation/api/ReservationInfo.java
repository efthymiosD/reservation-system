package com.decoupledx.reservation.reservation.api;

import java.time.Duration;
import java.time.Instant;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.reservation.api.ReservationId;
import com.decoupledx.reservation.resource.api.ResourceId;
import com.decoupledx.reservation.shared.domain.Money;

public record ReservationInfo(
        ReservationId id,
        ResourceId resourceId,
        CustomerId customerId,
        Instant start,
        Instant end,
        ReservationStatus status,
        Money price,
        Instant createdAt,
        Instant cancelledAt) {

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    /**
     * Whether this reservation may be cancelled at the given instant under a
     * cancellation policy with the given deadline before the start. View-oriented
     * UX hint; the cancel use case revalidates against the current policy.
     */
    public boolean isCancellable(Instant now, Duration deadlineBeforeStart) {
        return isActive() && !now.plus(deadlineBeforeStart).isAfter(start);
    }
}

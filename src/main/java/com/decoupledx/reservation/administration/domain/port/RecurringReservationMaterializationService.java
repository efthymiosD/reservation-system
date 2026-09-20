package com.decoupledx.reservation.administration.domain.port;

import com.decoupledx.reservation.administration.adapter.api.RecurringReservationMaterializationSummary;

import java.util.UUID;

public interface RecurringReservationMaterializationService {

    /**
     * Materializes every active recurring reservation up to its booking window.
     */
    RecurringReservationMaterializationSummary materializeDue();

    /**
     * Materializes a single recurringReservation (called right after creation so the
     * customer sees upcoming reservations immediately). Runs inside the current
     * transaction when invoked from another service.
     */
    void materializeFor(UUID recurringReservationId);
}

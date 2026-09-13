package com.decoupledx.reservation.administration.api;

import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.identity.api.CustomerId;

/**
 * Module API of the administration module: recurring per-customer reservations
 * (create with validation, cancel-whole-recurring-reservation) plus the materializer that
 * turns upcoming occurrences into real reservations. The only type other
 * modules may depend on.
 */
public interface AdministrationApi {

    RecurringReservationInfo createRecurringReservation(CreateRecurringReservationCommand command);

    void cancelRecurringReservation(UUID recurringReservationId, CustomerId actor);

    List<RecurringReservationInfo> findRecurringReservations();

    /**
     * Materializes every active recurring reservation's upcoming occurrences inside the
     * booking window. Safe to run repeatedly; the per-recurring-reservation cursor only
     * moves forward.
     */
    RecurringReservationMaterializationSummary materializeDue();
}
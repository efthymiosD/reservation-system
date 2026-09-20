package com.decoupledx.reservation.administration.adapter.api;

import com.decoupledx.reservation.administration.domain.port.RecurringReservationMaterializationService;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Module external facing module facade implementing {@link AdministrationApi} on top of the
 * recurring-reservation and materialization use-case services. The only bean other modules
 * use for this module.
 */
@RequiredArgsConstructor
class AdministrationApiImpl implements AdministrationApi {

    private final RecurringReservationService recurringReservationService;
    private final RecurringReservationMaterializationService materializationService;

    @Override
    public void createRecurringReservation(CreateRecurringReservationCommand command) {
        recurringReservationService.create(command);
    }

    @Override
    public void cancelRecurringReservation(UUID recurringReservationId) {
        recurringReservationService.cancel(recurringReservationId);
    }

    @Override
    public List<RecurringReservationInfo> findRecurringReservations() {
        return recurringReservationService.findAll();
    }

    @Override
    public RecurringReservationMaterializationSummary materializeDue() {
        return materializationService.materializeDue();
    }
}
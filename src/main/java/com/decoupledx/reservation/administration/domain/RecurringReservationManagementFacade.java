package com.decoupledx.reservation.administration.domain;

import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.administration.adapter.api.AdministrationApi;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationMaterializationSummary;
import com.decoupledx.reservation.administration.adapter.api.CreateRecurringReservationCommand;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;

import lombok.RequiredArgsConstructor;

/**
 * Module-internal facade implementing {@link AdministrationApi} on top of the
 * recurring-reservation and materialization use-case services. The only bean other modules
 * use for this module.
 */
@RequiredArgsConstructor
public class RecurringReservationManagementFacade implements AdministrationApi {

    private final RecurringReservationService recurringReservationService;
    private final RecurringReservationMaterializationService materializationService;

    @Override
    public RecurringReservationInfo createRecurringReservation(CreateRecurringReservationCommand command) {
        return recurringReservationService.create(command);
    }

    @Override
    public void cancelRecurringReservation(UUID recurringReservationId, CustomerId actor) {
        recurringReservationService.cancel(recurringReservationId, actor);
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
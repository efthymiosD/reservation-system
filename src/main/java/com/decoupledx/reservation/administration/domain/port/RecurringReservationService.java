package com.decoupledx.reservation.administration.domain.port;

import com.decoupledx.reservation.administration.adapter.api.CreateRecurringReservationCommand;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;

import java.util.List;
import java.util.UUID;

public interface RecurringReservationService {

    RecurringReservationInfo create(CreateRecurringReservationCommand command);

    /**
     * Cancels the whole recurring reservation and every not-yet-started reservation it has
     * produced, all in one transaction.
     */
    void cancel(UUID recurringReservationId);

    List<RecurringReservationInfo> findAll();

    List<RecurringReservationInfo> findByStatus(RecurringReservationStatus status);
}

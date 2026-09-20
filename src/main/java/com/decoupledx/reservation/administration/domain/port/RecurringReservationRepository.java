package com.decoupledx.reservation.administration.domain.port;

import com.decoupledx.reservation.administration.adapter.persistence.RecurringReservationDataValue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringReservationRepository {

    RecurringReservationDataValue save(RecurringReservationDataValue recurringReservation);

    Optional<RecurringReservationDataValue> findById(UUID id);

    List<RecurringReservationDataValue> findAll();

    List<RecurringReservationDataValue> findAllActive();
}
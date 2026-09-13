package com.decoupledx.reservation.administration.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.decoupledx.reservation.administration.domain.model.RecurringReservation;

public interface RecurringReservationRepository {

    RecurringReservation save(RecurringReservation recurringReservation);

    Optional<RecurringReservation> findById(UUID id);

    List<RecurringReservation> findAll();

    List<RecurringReservation> findAllActive();
}
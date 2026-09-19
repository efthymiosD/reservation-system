package com.decoupledx.reservation.reservation.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.model.Reservation;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.ReservationPeriod;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(ReservationId id);

    List<Reservation> findByCustomer(CustomerId customerId);

    List<Reservation> findByCustomer(CustomerId customerId, ReservationStatus status, int page, int size);

    long countByCustomer(CustomerId customerId, ReservationStatus status);

    List<Reservation> findActiveOverlappingResource(ResourceId resourceId, ReservationPeriod period);

    List<Reservation> findActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period);

    boolean existsActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period);

    List<Reservation> findActiveByRecurringReservationId(UUID recurringReservationId);

    List<Reservation> findAll(ReservationStatus status, int page, int size);

    long countAll(ReservationStatus status);
}

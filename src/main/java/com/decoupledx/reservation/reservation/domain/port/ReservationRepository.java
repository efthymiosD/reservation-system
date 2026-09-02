package com.decoupledx.reservation.reservation.domain.port;

import java.util.List;
import java.util.Optional;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.model.ReservationId;
import com.decoupledx.reservation.reservation.domain.model.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.model.Reservation;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(ReservationId id);

    List<Reservation> findByCustomer(CustomerId customerId);

    List<Reservation> findByCustomer(CustomerId customerId, ReservationStatus status, int page, int size);

    long countByCustomer(CustomerId customerId, ReservationStatus status);

    List<Reservation> findActiveOverlappingResource(ResourceId resourceId, ReservationPeriod period);

    boolean existsActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period);

    List<Reservation> findAll(ReservationStatus status, int page, int size);

    long countAll(ReservationStatus status);
}

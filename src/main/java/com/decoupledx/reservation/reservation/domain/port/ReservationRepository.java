package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.adapter.persistence.ReservationDataValue;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.ReservationPeriod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    ReservationDataValue save(ReservationDataValue reservation);

    Optional<ReservationDataValue> findById(ReservationId id);

    List<ReservationDataValue> findByCustomer(CustomerId customerId);

    List<ReservationDataValue> findByCustomer(CustomerId customerId, ReservationStatus status, int page, int size);

    long countByCustomer(CustomerId customerId, ReservationStatus status);

    List<ReservationDataValue> findActiveOverlappingResource(ResourceId resourceId, ReservationPeriod period);

    List<ReservationDataValue> findActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period);

    boolean existsActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period);

    List<ReservationDataValue> findActiveByRecurringReservationId(UUID recurringReservationId);

    List<ReservationDataValue> findAll(ReservationStatus status, int page, int size);

    long countAll(ReservationStatus status);
}

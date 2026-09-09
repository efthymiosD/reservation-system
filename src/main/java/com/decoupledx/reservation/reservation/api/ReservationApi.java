package com.decoupledx.reservation.reservation.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

/**
 * Module API of the reservation module: the booking use cases and the queries
 * other modules need (availability checking, personal listings). This is the
 * only type other modules may depend on; the implementation stays
 * module-internal.
 */
public interface ReservationApi {

    ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes, CustomerId customer);

    void cancel(UUID reservationId, CustomerId customer);

    ReservationInfo getReservation(UUID reservationId, CustomerId customer);

    ReservationPage findMyReservationsPage(CustomerId customer, ReservationStatus status, int page, int size);

    List<ReservationInfo> findActiveOverlappingCustomer(CustomerId customer, ReservationPeriod period);

    List<ReservationInfo> findActiveOverlappingResource(UUID resourceId, ReservationPeriod period);

    boolean isSlotFree(UUID resourceId, ReservationPeriod period);

    void cancelAdministratively(UUID reservationId, CustomerId actor);

    record ReservationPage(List<ReservationInfo> items, long total, int page, int size) {
    }
}

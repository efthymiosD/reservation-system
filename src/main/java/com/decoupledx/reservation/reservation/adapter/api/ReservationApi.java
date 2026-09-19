package com.decoupledx.reservation.reservation.adapter.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.shared.ReservationPeriod;

/**
 * Module API of the reservation module: the booking use cases and the queries
 * other modules need (availability checking, personal listings). This is the
 * only type other modules may depend on; the implementation stays
 * module-internal.
 */
public interface ReservationApi {

    ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes, CustomerId customer);

    /**
     * Creates a reservation on behalf of a recurring reservation (used by the
     * recurring-reservation materializer). The produced reservation is linked to the
     * recurring reservation via its {@code recurringReservationId}.
     */
    void createForRecurringReservation(UUID resourceId, LocalDateTime startTime, int durationMinutes,
                                       CustomerId customer, UUID recurringReservationId);

    void cancel(UUID reservationId, CustomerId customer);

    ReservationInfo getReservation(UUID reservationId, CustomerId customer);

    ReservationPage findMyReservationsPage(CustomerId customer, ReservationStatus status, int page, int size);

    List<ReservationInfo> findActiveOverlappingCustomer(CustomerId customer, ReservationPeriod period);

    List<ReservationInfo> findActiveOverlappingResource(UUID resourceId, ReservationPeriod period);

    List<ReservationInfo> findActiveByRecurringReservation(UUID recurringReservationId);

    boolean isSlotFree(UUID resourceId, ReservationPeriod period);

    ReservationPage listAllForAdmin(ReservationStatus status, int page, int size);

    void cancelAdministratively(UUID reservationId, CustomerId actor);
}

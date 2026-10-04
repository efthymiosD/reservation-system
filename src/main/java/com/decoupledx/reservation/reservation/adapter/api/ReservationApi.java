package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.shared.ReservationPeriod;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Module API of the reservation module: the booking use cases and the queries
 * other modules need (availability checking, personal listings). This is the
 * only type other modules may depend on; the implementation stays
 * module-internal.
 */
public interface ReservationApi {

    /**
     * Customer-facing entry point for the current player (ADR 0002): the
     * customer is resolved inside the module from the session context.
     */
    ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes);

    ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes, CustomerId customer);

    /**
     * Creates a reservation on behalf of a recurring reservation (used by the
     * recurring-reservation materializer). The produced reservation is linked to the
     * recurring reservation via its {@code recurringReservationId}.
     */
    void createForRecurringReservation(UUID resourceId, LocalDateTime startTime, int durationMinutes,
                                       CustomerId customer, UUID recurringReservationId);

    /**
     * Customer-facing cancel for the current player (ADR 0002).
     */
    void cancel(UUID reservationId);

    void cancel(UUID reservationId, CustomerId customer);

    /**
     * Customer-facing read for the current player (ADR 0002).
     */
    ReservationInfo getReservation(UUID reservationId);

    ReservationInfo getReservation(UUID reservationId, CustomerId customer);

    /**
     * Customer-facing listing for the current player (ADR 0002).
     */
    ReservationPage findMyReservationsPage(ReservationStatus status, int page, int size);

    ReservationPage findMyReservationsPage(CustomerId customer, ReservationStatus status, int page, int size);

    /**
     * Overlap check against the current player's own reservations (ADR 0002).
     */
    List<ReservationInfo> findMyActiveOverlapping(ReservationPeriod period);

    List<ReservationInfo> findActiveOverlappingCustomer(CustomerId customer, ReservationPeriod period);

    List<ReservationInfo> findActiveOverlappingResource(UUID resourceId, ReservationPeriod period);

    List<ReservationInfo> findActiveByRecurringReservation(UUID recurringReservationId);

    boolean isSlotFree(UUID resourceId, ReservationPeriod period);

    ReservationPage listAllForAdmin(ReservationStatus status, int page, int size);

    /**
     * Administrative cancel by the current player acting as administrator (ADR 0002).
     */
    void cancelAdministratively(UUID reservationId);

    void cancelAdministratively(UUID reservationId, CustomerId actor);
}

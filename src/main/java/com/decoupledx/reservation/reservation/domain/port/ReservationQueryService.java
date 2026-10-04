package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.shared.ReservationPeriod;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port: reservation queries for customers and other modules.
 */
public interface ReservationQueryService {

    /**
     * Customer-facing entry point for the current player (ADR 0002).
     */
    ReservationInfo getReservation(UUID reservationId);

    ReservationInfo getReservation(UUID reservationId, CustomerId customer);

    /**
     * Customer-facing entry point for the current player (ADR 0002).
     */
    ReservationPage findMyReservationsPage(ReservationStatus status, int page, int size);

    ReservationPage findMyReservationsPage(CustomerId customer, ReservationStatus status, int page, int size);

    /**
     * Overlap check for the current player's own reservations (ADR 0002).
     */
    List<ReservationInfo> findMyActiveOverlapping(ReservationPeriod period);

    List<ReservationInfo> findActiveOverlappingCustomer(CustomerId customer, ReservationPeriod period);

    List<ReservationInfo> findActiveOverlappingResource(UUID resourceId, ReservationPeriod period);

    List<ReservationInfo> findActiveByRecurringReservation(UUID recurringReservationId);

    ReservationPage listAllForAdmin(ReservationStatus status, int page, int size);

    boolean isSlotFree(UUID resourceId, ReservationPeriod period);
}

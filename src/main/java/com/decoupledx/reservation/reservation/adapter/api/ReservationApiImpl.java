package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.domain.port.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import com.decoupledx.reservation.shared.ReservationPeriod;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link ReservationApi} on top of
 * the reservation inbound ports. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class ReservationApiImpl implements ReservationApi {

    private final CreateReservationService createReservation;
    private final CancelReservationService cancelReservation;
    private final ReservationQueryService reservationQueries;

    @Override
    public ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes) {
        return createReservation.create(resourceId, startTime, durationMinutes);
    }

    @Override
    public ReservationInfo createOnBehalfOfCustomer(UUID resourceId, LocalDateTime startTime,
                                                    int durationMinutes, CustomerId customer) {
        return createReservation.createOnBehalfOfCustomer(resourceId, startTime, durationMinutes, customer);
    }

    @Override
    public void createForRecurringReservation(UUID resourceId, LocalDateTime startTime, int durationMinutes,
                                              CustomerId customer, UUID recurringReservationId) {
        createReservation.createForRecurringReservation(resourceId, startTime, durationMinutes,
                customer, recurringReservationId);
    }

    @Override
    public void cancel(UUID reservationId) {
        cancelReservation.cancel(reservationId);
    }

    @Override
    public void cancel(UUID reservationId, CustomerId customer) {
        cancelReservation.cancel(reservationId, customer);
    }

    @Override
    public void cancelAdministratively(UUID reservationId) {
        cancelReservation.cancelAdministratively(reservationId);
    }

    @Override
    public void cancelAdministratively(UUID reservationId, CustomerId actor) {
        cancelReservation.cancelAdministratively(reservationId, actor);
    }

    @Override
    public ReservationInfo getReservation(UUID reservationId) {
        return reservationQueries.getReservation(reservationId);
    }

    @Override
    public ReservationInfo getReservation(UUID reservationId, CustomerId customer) {
        return reservationQueries.getReservation(reservationId, customer);
    }

    @Override
    public ReservationPage findMyReservationsPage(ReservationStatus status, int page, int size) {
        return reservationQueries.findMyReservationsPage(status, page, size);
    }

    @Override
    public ReservationPage findMyReservationsPage(CustomerId customer, ReservationStatus status,
                                                  int page, int size) {
        return reservationQueries.findMyReservationsPage(customer, status, page, size);
    }

    @Override
    public List<ReservationInfo> findMyActiveOverlapping(ReservationPeriod period) {
        return reservationQueries.findMyActiveOverlapping(period);
    }

    @Override
    public List<ReservationInfo> findActiveOverlappingCustomer(CustomerId customer, ReservationPeriod period) {
        return reservationQueries.findActiveOverlappingCustomer(customer, period);
    }

    @Override
    public List<ReservationInfo> findActiveOverlappingResource(UUID resourceId, ReservationPeriod period) {
        return reservationQueries.findActiveOverlappingResource(resourceId, period);
    }

    @Override
    public List<ReservationInfo> findActiveByRecurringReservation(UUID recurringReservationId) {
        return reservationQueries.findActiveByRecurringReservation(recurringReservationId);
    }

    @Override
    public ReservationPage listAllForAdmin(ReservationStatus status, int page, int size) {
        return reservationQueries.listAllForAdmin(status, page, size);
    }

    @Override
    public boolean isSlotFree(UUID resourceId, ReservationPeriod period) {
        return reservationQueries.isSlotFree(resourceId, period);
    }
}

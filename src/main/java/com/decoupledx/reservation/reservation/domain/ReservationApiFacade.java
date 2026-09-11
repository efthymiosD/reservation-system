package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.reservation.api.ReservationId;
import com.decoupledx.reservation.reservation.api.ReservationInfo;
import com.decoupledx.reservation.reservation.api.ReservationPage;
import com.decoupledx.reservation.reservation.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.service.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.service.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ReservationApiFacade implements ReservationApi {

    private final CreateReservationService createReservation;
    private final CancelReservationService cancelReservation;
    private final ReservationQueryService reservationQueries;

    @Override
    public ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes,
                                  CustomerId customer) {
        return createReservation.create(resourceId, startTime, durationMinutes, customer);
    }

    @Override
    public void cancel(UUID reservationId, CustomerId customer) {
        cancelReservation.cancel(ReservationId.of(reservationId), customer);
    }

    @Override
    public void cancelAdministratively(UUID reservationId, CustomerId actor) {
        cancelReservation.cancelAdministratively(ReservationId.of(reservationId), actor);
    }

    @Override
    public ReservationInfo getReservation(UUID reservationId, CustomerId customer) {
        return reservationQueries.getReservation(ReservationId.of(reservationId), customer);
    }

    @Override
    public ReservationPage findMyReservationsPage(CustomerId customer, ReservationStatus status,
                                                  int page, int size) {
        ReservationQueryService.ReservationPage result =
                reservationQueries.findMyReservationsPage(customer, status, page, size);
        return new ReservationPage(result.items(), result.total(), result.page(), result.size());
    }

    @Override
    public List<ReservationInfo> findActiveOverlappingCustomer(CustomerId customer, ReservationPeriod period) {
        return reservationQueries.findActiveOverlappingCustomer(customer, period);
    }

    @Override
    public List<ReservationInfo> findActiveOverlappingResource(UUID resourceId, ReservationPeriod period) {
        return reservationQueries.findActiveOverlappingResource(
                com.decoupledx.reservation.resource.api.ResourceId.of(resourceId), period);
    }

    @Override
    public ReservationPage listAllForAdmin(ReservationStatus status, int page, int size) {
        ReservationQueryService.ReservationPage result =
                reservationQueries.findAllForAdmin(status, page, size);
        return new ReservationPage(result.items(), result.total(), result.page(), result.size());
    }

    @Override
    public boolean isSlotFree(UUID resourceId, ReservationPeriod period) {
        return reservationQueries.findActiveOverlappingResource(
                com.decoupledx.reservation.resource.api.ResourceId.of(resourceId), period).isEmpty();
    }
}

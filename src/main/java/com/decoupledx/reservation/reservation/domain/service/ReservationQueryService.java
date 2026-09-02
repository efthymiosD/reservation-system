package com.decoupledx.reservation.reservation.domain.service;

import java.util.List;
import com.decoupledx.reservation.reservation.domain.model.ReservationId;
import com.decoupledx.reservation.reservation.domain.model.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.model.ReservationStatus;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.model.Reservation;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReservationQueryService {

    private final ReservationRepository reservations;

    public List<ReservationInfo> findMyReservations(CustomerId customerId) {
        return reservations.findByCustomer(customerId).stream()
                .map(CreateReservationService::toInfo)
                .toList();
    }

    public ReservationPage findMyReservationsPage(CustomerId customerId, ReservationStatus status, int page, int size) {
        List<ReservationInfo> items = reservations.findByCustomer(customerId, status, page, size).stream()
                .map(CreateReservationService::toInfo)
                .toList();
        return new ReservationPage(items, reservations.countByCustomer(customerId, status), Math.max(page, 0), size);
    }

    public ReservationInfo getReservation(ReservationId reservationId, CustomerId customerId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!reservation.getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return CreateReservationService.toInfo(reservation);
    }

    public List<ReservationInfo> findActiveOverlappingResource(ResourceId resourceId, ReservationPeriod period) {
        return reservations.findActiveOverlappingResource(resourceId, period).stream()
                .map(CreateReservationService::toInfo)
                .toList();
    }

    public ReservationPage findAllForAdmin(ReservationStatus status, int page, int size) {
        List<ReservationInfo> items = reservations.findAll(status, page, size).stream()
                .map(CreateReservationService::toInfo)
                .toList();
        return new ReservationPage(items, reservations.countAll(status), Math.max(page, 0), size);
    }

    public record ReservationPage(List<ReservationInfo> items, long total, int page, int size) {
    }
}

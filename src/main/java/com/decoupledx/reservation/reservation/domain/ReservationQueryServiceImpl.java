package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.adapter.persistence.ReservationDataValue;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.ReservationPeriod;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ReservationQueryServiceImpl implements ReservationQueryService {

    private final ReservationRepository reservations;
    private final CurrentCustomerApi currentCustomers;

    @Override
    public ReservationInfo getReservation(UUID reservationId) {
        return getReservation(reservationId, currentCustomers.currentCustomerId());
    }

    @Override
    public ReservationPage findMyReservationsPage(ReservationStatus status, int page, int size) {
        return findMyReservationsPage(currentCustomers.currentCustomerId(), status, page, size);
    }

    @Override
    public ReservationInfo getReservation(UUID reservationId, CustomerId customerId) {
        ReservationDataValue data = reservations.findById(ReservationId.of(reservationId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!CustomerId.of(data.customerId()).equals(customerId)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return ReservationMappings.toInfo(data);
    }

    @Override
    public ReservationPage findMyReservationsPage(CustomerId customerId, ReservationStatus status, int page, int size) {
        List<ReservationInfo> items = reservations.findByCustomer(customerId, status, page, size).stream()
                .map(ReservationMappings::toInfo)
                .toList();
        return new ReservationPage(items, reservations.countByCustomer(customerId, status), Math.max(page, 0), size);
    }

    @Override
    public List<ReservationInfo> findActiveOverlappingResource(UUID resourceId, ReservationPeriod period) {
        return reservations.findActiveOverlappingResource(ResourceId.of(resourceId), period).stream()
                .map(ReservationMappings::toInfo)
                .toList();
    }

    @Override
    public List<ReservationInfo> findMyActiveOverlapping(ReservationPeriod period) {
        // Administrators may hold several same-hour bookings for themselves
        // (their placements are exempt from the customer-overlap invariant),
        // so the "you already hold this slot" hint and its blocked Reserve
        // button must not apply to them.
        if (currentCustomers.isAdministrator()) {
            return List.of();
        }
        return findActiveOverlappingCustomer(currentCustomers.currentCustomerId(), period);
    }

    @Override
    public List<ReservationInfo> findActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period) {
        return reservations.findActiveOverlappingCustomer(customerId, period).stream()
                .map(ReservationMappings::toInfo)
                .toList();
    }

    @Override
    public List<ReservationInfo> findActiveByRecurringReservation(UUID recurringReservationId) {
        return reservations.findActiveByRecurringReservationId(recurringReservationId).stream()
                .map(ReservationMappings::toInfo)
                .toList();
    }

    @Override
    public ReservationPage listAllForAdmin(ReservationStatus status, int page, int size) {
        List<ReservationInfo> items = reservations.findAll(status, page, size).stream()
                .map(ReservationMappings::toInfo)
                .toList();
        return new ReservationPage(items, reservations.countAll(status), Math.max(page, 0), size);
    }

    @Override
    public boolean isSlotFree(UUID resourceId, ReservationPeriod period) {
        return reservations.findActiveOverlappingResource(ResourceId.of(resourceId), period).isEmpty();
    }
}

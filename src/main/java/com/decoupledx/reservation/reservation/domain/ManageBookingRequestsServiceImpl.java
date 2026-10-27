package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerDirectoryApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestPage;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestStatus;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.port.BookingRequestRepository;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ManageBookingRequestsService;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * The coach-side decisions. Approval is AUTHORITATIVE: it provisions the
 * customer's internal identity, calls the existing staff-placement creation use
 * case (the same-module port — the resource invariant and the exclusion
 * constraints stay authoritative), and links the reservation to the request
 * in one transaction. Any conflict (slot taken, resource gone) leaves the
 * request PENDING — the slot was never held — and surfaces as the use case's
 * business failure.
 */
@Slf4j
@RequiredArgsConstructor
class ManageBookingRequestsServiceImpl implements ManageBookingRequestsService {

    private final BookingRequestRepository bookingRequests;
    private final CreateReservationService createReservation;
    private final CurrentCustomerApi currentCustomers;
    private final CustomerDirectoryApi customerDirectory;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public BookingRequestPage list(BookingRequestStatus status, int page, int size) {
        List<BookingRequestInfo> items = BookingRequestMappings.toInfos(
                bookingRequests.findPage(status, Math.max(page, 0), size));
        return new BookingRequestPage(items, bookingRequests.count(status), Math.max(page, 0), size);
    }

    @Override
    public BookingRequestInfo approve(UUID requestId) {
        return tx.run(() -> doApprove(requestId));
    }

    @Override
    public BookingRequestInfo reject(UUID requestId) {
        return tx.run(() -> doReject(requestId));
    }

    private BookingRequestInfo doApprove(UUID requestId) {
        BookingRequest request = pendingRequest(requestId);
        String actor = currentCustomers.currentCustomerId().value();

        // The customer has no IdP session: provision the app-owned internal
        // identity from the request's own data (ADR-0002's auto-provisioning,
        // via the identity module API), then place the booking like any staff
        // placement.
        CustomerId customerId = customerDirectory.provisionExternalCustomer(
                externalSubjectOf(request.getId()), request.getCustomerName());
        ReservationInfo reservation = createReservation.createOnBehalfOfCustomer(
                request.getResourceId().value(), request.getStartAt(),
                request.getDurationMinutes(), customerId);

        request.approve(clock.instant(), actor, reservation.id().value());
        bookingRequests.save(request.toDataValue());
        log.info("Booking request approved id={} reservationId={} actor={}", requestId, reservation.id(), actor);
        return BookingRequestMappings.toInfo(request.toDataValue());
    }

    private BookingRequestInfo doReject(UUID requestId) {
        BookingRequest request = pendingRequest(requestId);
        String actor = currentCustomers.currentCustomerId().value();

        request.reject(clock.instant(), actor);
        bookingRequests.save(request.toDataValue());
        log.info("Booking request rejected id={} actor={}", requestId, actor);
        return BookingRequestMappings.toInfo(request.toDataValue());
    }

    private BookingRequest pendingRequest(UUID requestId) {
        return bookingRequests.findById(requestId)
                .map(BookingRequest::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_REQUEST_NOT_FOUND));
    }

    /**
     * Workflow-namespaced subject for provisioned external customers.
     */
    private static String externalSubjectOf(UUID requestId) {
        return "wa-link:" + requestId;
    }
}

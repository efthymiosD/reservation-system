package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.reservation.domain.port.BookingRequestLinkService;
import com.decoupledx.reservation.reservation.domain.port.ManageBookingRequestsService;
import com.decoupledx.reservation.reservation.domain.port.SubmitBookingRequestService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

/**
 * Module external-facing facade implementing {@link BookingRequestApi} on top
 * of the booking-request inbound ports. The only bean other modules use for
 * the workflow.
 */
@RequiredArgsConstructor
class BookingRequestApiImpl implements BookingRequestApi {

    private final SubmitBookingRequestService submitBookingRequest;
    private final ManageBookingRequestsService manageBookingRequests;
    private final BookingRequestLinkService linkService;

    @Override
    public UUID resolveLink(String rawToken) {
        return linkService.resolveLinkId(rawToken);
    }

    @Override
    public BookingRequestInfo submit(UUID linkId, SubmitBookingRequestCommand command) {
        return submitBookingRequest.submit(linkId, command);
    }

    @Override
    public Optional<BookingRequestInfo> bySubmissionKey(String rawToken, String submissionKey) {
        return submitBookingRequest.bySubmissionKey(linkService.resolveLinkId(rawToken), submissionKey);
    }

    @Override
    public BookingRequestPage listForAdmin(BookingRequestStatus status, int page, int size) {
        return manageBookingRequests.list(status, page, size);
    }

    @Override
    public BookingRequestInfo approve(UUID requestId) {
        return manageBookingRequests.approve(requestId);
    }

    @Override
    public BookingRequestInfo reject(UUID requestId) {
        return manageBookingRequests.reject(requestId);
    }

    @Override
    public Optional<BookingRequestLinkInfo> activeLink() {
        return linkService.activeLink();
    }

    @Override
    public BookingRequestLinkToken issueNewLink(String label) {
        return linkService.issueNewLink(label);
    }

    @Override
    public void revokeActiveLink() {
        linkService.revokeActive();
    }
}

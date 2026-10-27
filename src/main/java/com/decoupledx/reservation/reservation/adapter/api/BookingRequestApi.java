package com.decoupledx.reservation.reservation.adapter.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Module API of the booking-request workflow (coach shares a link, a customer
 * submits, the coach approves/rejects). PENDING requests never reserve or
 * block; approval is authoritative and goes through the existing reservation
 * creation use case.
 */
public interface BookingRequestApi {

    /**
     * Boolean link resolution for the public flow: the link id for a raw
     * token, or the module's link failures (unknown/revoked).
     */
    UUID resolveLink(String rawToken);

    /**
     * Public-form submission; validates server-side, returns the PENDING request.
     */
    BookingRequestInfo submit(UUID linkId, SubmitBookingRequestCommand command);

    /**
     * Acknowledgement lookup for the public flow; empty when unknown or foreign to the link.
     */
    Optional<BookingRequestInfo> bySubmissionKey(String rawToken, String submissionKey);

    BookingRequestPage listForAdmin(BookingRequestStatus status, int page, int size);

    BookingRequestInfo approve(UUID requestId);

    BookingRequestInfo reject(UUID requestId);

    Optional<BookingRequestLinkInfo> activeLink();

    /**
     * Returns a token whose raw value must be displayed to the coach ONCE.
     */
    BookingRequestLinkToken issueNewLink(String label);

    /**
     * Revokes the currently reusable link (if any).
     */
    void revokeActiveLink();
}

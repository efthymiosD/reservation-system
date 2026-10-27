package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestPage;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestStatus;

import java.util.UUID;

public interface ManageBookingRequestsService {

    BookingRequestPage list(BookingRequestStatus status, int page, int size);

    /**
     * Authoritative conversion of a PENDING request into a reservation: the
     * creation use case revalidates availability under the current policy and
     * level constraints. On any conflict the request stays PENDING (the slot
     * was never held) and the failure surfaces to the caller.
     */
    BookingRequestInfo approve(UUID requestId);

    BookingRequestInfo reject(UUID requestId);
}

package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.SubmitBookingRequestCommand;

import java.util.Optional;
import java.util.UUID;

public interface SubmitBookingRequestService {

    /**
     * Validates the request against the venue's own rules (advance window,
     * opening hours, slot grid, supported durations, active resource — the
     * same shared component the reservation creation uses, nothing
     * re-implemented) and persists it as PENDING. Never creates a
     * reservation or blocks a slot. Re-submitting the same
     * {@code submissionKey} returns the original request (browser
     * refresh/back-button is idempotent); a foreign key for the link fails.
     */
    BookingRequestInfo submit(UUID linkId, SubmitBookingRequestCommand command);

    /**
     * Public acknowledgement lookup: the request the submission key belongs
     * to, only within the same link; {@code Optional.empty()} on unknown.
     */
    Optional<BookingRequestInfo> bySubmissionKey(UUID linkId, String submissionKey);
}

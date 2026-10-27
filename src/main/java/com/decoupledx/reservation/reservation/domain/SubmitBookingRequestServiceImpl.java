package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.SubmitBookingRequestCommand;
import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestDataValue;
import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestLinkDataValue;
import com.decoupledx.reservation.reservation.domain.port.BookingRequestLinkRepository;
import com.decoupledx.reservation.reservation.domain.port.BookingRequestRepository;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import com.decoupledx.reservation.reservation.domain.port.SubmitBookingRequestService;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The public-form submission use case. PENDING = a statement of intent: sits
 * in its own table, reserves nothing, blocks nothing. Structural validation
 * reuses the shared slot rules (no rule is re-implemented here); the free-at-
 * submission snapshot is advisory context for the coach, not a promise.
 */
@Slf4j
@RequiredArgsConstructor
class SubmitBookingRequestServiceImpl implements SubmitBookingRequestService {

    private final BookingRequestRepository bookingRequests;
    private final BookingRequestLinkRepository links;
    private final ResourceApi resourceService;
    private final VenueApi venueService;
    private final SlotPeriodRules slotPeriodRules;
    private final ReservationQueryService reservationQueries;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public BookingRequestInfo submit(UUID linkId, SubmitBookingRequestCommand command) {
        return tx.run(() -> doSubmit(linkId, command));
    }

    @Override
    public Optional<BookingRequestInfo> bySubmissionKey(UUID linkId, String submissionKey) {
        if (submissionKey == null || submissionKey.isBlank()) {
            return Optional.empty();
        }
        return bookingRequests.findBySubmissionKey(submissionKey)
                .filter(data -> data.linkId().equals(linkId))
                .map(BookingRequestMappings::toInfo);
    }

    private BookingRequestInfo doSubmit(UUID linkId, SubmitBookingRequestCommand command) {
        // The caller resolved the raw token; activeness is re-checked here —
        // the link could have been revoked between the page load and submit.
        BookingRequestLinkDataValue active = links.findActive()
                .filter(current -> current.id().equals(linkId))
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_REQUEST_LINK_REVOKED));

        Optional<BookingRequestDataValue> duplicate =
                bookingRequests.findBySubmissionKey(command.submissionKey());
        if (duplicate.isPresent()) {
            BookingRequestDataValue original = duplicate.get();
            if (!active.id().equals(original.linkId())) {
                // The key is minted token-hash prefixed AND validated here:
                // a foreign submission key resolves to nobody's request.
                throw new BusinessException(ErrorCode.BOOKING_REQUEST_LINK_INVALID);
            }
            return infoOf(original);
        }

        ResourceInfo resource = resourceService.getResource(command.resourceId());
        if (!resource.isActive()) {
            throw new BusinessException(ErrorCode.RESOURCE_INACTIVE);
        }

        VenueInfo venue = venueService.getVenue(resource.venueId().value());
        Instant start = command.startAt().atZone(venue.timezone()).toInstant();
        Instant end = start.plus(Duration.ofMinutes(command.durationMinutes()));
        ReservationPeriod period = ReservationPeriod.of(start, end);
        slotPeriodRules.check(start, period, venue, false);

        boolean isFree = reservationQueries.isSlotFree(command.resourceId(), period);

        BookingRequest request = BookingRequest.create(
                ResourceId.of(command.resourceId()), command.startAt(), command.durationMinutes(),
                command.customerName(), command.contact(), command.note(),
                active.id(), command.submissionKey(), isFree, clock.instant());
        BookingRequestDataValue saved = bookingRequests.save(request.toDataValue());
        log.info("Booking request submitted id={} ref={} resourceId={} freeAtSubmission={}",
                saved.id(), infoOf(saved).reference(), saved.resourceId(), saved.advisoryFreeAtSubmission());
        return infoOf(saved);
    }

    private BookingRequestInfo infoOf(BookingRequestDataValue data) {
        return BookingRequestMappings.toInfo(data);
    }
}

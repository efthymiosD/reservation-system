package com.decoupledx.reservation.reservation.adapter.persistence;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestStatus;
import com.decoupledx.reservation.reservation.domain.port.BookingRequestLinkRepository;
import com.decoupledx.reservation.reservation.domain.port.BookingRequestRepository;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapters of the booking-request workflow. Decisions are
 * optimistic-version-guarded: a concurrent Approve/Reject (both having read
 * the same PENDING row) fails on the version — translated to the module's
 * friendly already-decided failure instead of a 500.
 */
@Component
@RequiredArgsConstructor
class BookingRequestPersistenceAdapter implements BookingRequestRepository, BookingRequestLinkRepository {

    private final BookingRequestJpaRepository bookingRequests;
    private final BookingRequestLinkJpaRepository links;

    @Override
    public BookingRequestDataValue save(BookingRequestDataValue request) {
        try {
            bookingRequests.findById(request.id()).ifPresentOrElse(
                    // Known row: a decision is being applied (@Version guards concurrency).
                    entity -> entity.applyDecision(request.status(), request.decidedAt(),
                            request.decidedBy(), request.reservationId(),
                            request.updatedAt()),
                    () -> bookingRequests.save(toEntity(request)));
            bookingRequests.flush();
            return request;
        } catch (ObjectOptimisticLockingFailureException concurrentDecision) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_ALREADY_DECIDED);
        }
    }

    @Override
    public Optional<BookingRequestDataValue> findById(UUID id) {
        return bookingRequests.findById(id).map(this::toData);
    }

    @Override
    public Optional<BookingRequestDataValue> findBySubmissionKey(String submissionKey) {
        return bookingRequests.findBySubmissionKey(submissionKey)
                .map(this::toData);
    }

    @Override
    public List<BookingRequestDataValue> findPage(BookingRequestStatus status, int page, int size) {
        var pageRequest = PageRequest.of(Math.max(page, 0), size);
        var entities = status == null
                ? bookingRequests.findAllByOrderByCreatedAtDesc(pageRequest)
                : bookingRequests.findByStatusOrderByCreatedAtDesc(status.name(), pageRequest);
        return entities.map(this::toData).getContent();
    }

    @Override
    public long count(BookingRequestStatus status) {
        return status == null ? bookingRequests.count() : bookingRequests.countByStatus(status.name());
    }

    @Override
    public BookingRequestLinkDataValue save(BookingRequestLinkDataValue link) {
        try {
            links.findById(link.id()).ifPresentOrElse(entity -> {
                if (link.revokedAt() != null) {
                    entity.applyRevocation(link.revokedAt());
                }
            }, () -> links.save(new BookingRequestLinkEntity(
                    link.id(), link.label(), link.tokenHash(), link.createdAt())));
            // Flush inside the catch: the insert/revocation must surface
            // integrity races HERE (not at commit time, outside this catch).
            links.flush();
            return link;
        } catch (DataIntegrityViolationException raceLostSingleActiveLink) {
            // The partial unique index validates the invariant; a
            // double-clicked regeneration is the loser.
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_LINK_CONFLICT);
        }
    }

    @Override
    public Optional<BookingRequestLinkDataValue> findByTokenHash(String tokenHash) {
        return links.findByTokenHash(tokenHash).map(this::toLinkData);
    }

    @Override
    public Optional<BookingRequestLinkDataValue> findActive() {
        return links.findByRevokedAtIsNull().map(this::toLinkData);
    }

    private BookingRequestLinkDataValue toLinkData(BookingRequestLinkEntity entity) {
        return new BookingRequestLinkDataValue(entity.getId(), entity.getLabel(),
                entity.getTokenHash(), entity.getCreatedAt(), entity.getRevokedAt());
    }

    private BookingRequestEntity toEntity(BookingRequestDataValue data) {
        return new BookingRequestEntity(data.id(), data.status(), UUID.fromString(data.resourceId()),
                data.startAt(), data.durationMinutes(), data.endAt(),
                data.customerName(), data.contact(), data.note(),
                data.linkId(), data.submissionKey(), data.advisoryFreeAtSubmission(),
                data.createdAt(), data.updatedAt());
    }

    private BookingRequestDataValue toData(BookingRequestEntity entity) {
        return new BookingRequestDataValue(entity.getId(), entity.getStatus(),
                entity.getResourceId().toString(), entity.getStartAt(),
                entity.getDurationMinutes(), entity.getEndAt(),
                entity.getCustomerName(), entity.getContact(), entity.getNote(),
                entity.getLinkId(), entity.getSubmissionKey(),
                entity.isAdvisoryFreeAtSubmission(), entity.getCreatedAt(),
                entity.getUpdatedAt(), entity.getDecidedAt(),
                entity.getDecidedBy(), entity.getReservationId());
    }
}
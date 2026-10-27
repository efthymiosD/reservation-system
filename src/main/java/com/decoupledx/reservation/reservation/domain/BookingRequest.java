package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestStatus;
import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestDataValue;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A booking REQUEST submitted by a customer through a coach's shareable link.
 * Static, language-agnostic model: owns its lifecycle transitions only
 * (PENDING → APPROVED/REJECTED/CANCELLED). Constructing the actual
 * reservation is NOT its job — approval services call the reservation use
 * case and attach the result here.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class BookingRequest {

    private UUID id;
    private BookingRequestStatus status;
    private ResourceId resourceId;
    /**
     * Venue-owner wall-clock start (same semantics as the reserve API input).
     */
    private LocalDateTime startAt;
    private int durationMinutes;
    private LocalDateTime endAt;
    private String customerName;
    private String contact;
    private String note;
    private UUID linkId;
    private String submissionKey;
    /**
     * Advisory: was the requested slot free at submission time.
     */
    private boolean advisoryFreeAtSubmission;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant decidedAt;
    private String decidedBy;
    private UUID reservationId;

    static BookingRequest create(ResourceId resourceId, LocalDateTime startAt,
                                 int durationMinutes, String customerName, String contact,
                                 String note, UUID linkId, String submissionKey,
                                 boolean advisoryFreeAtSubmission, Instant now) {
        BookingRequest request = new BookingRequest();
        request.id = UUID.randomUUID();
        request.status = BookingRequestStatus.PENDING;
        request.resourceId = resourceId;
        request.startAt = startAt;
        request.durationMinutes = durationMinutes;
        request.endAt = startAt.plusMinutes(durationMinutes);
        request.customerName = customerName;
        request.contact = contact;
        request.note = note;
        request.linkId = linkId;
        request.submissionKey = submissionKey;
        request.advisoryFreeAtSubmission = advisoryFreeAtSubmission;
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }

    static BookingRequest from(BookingRequestDataValue data) {
        BookingRequest request = new BookingRequest();
        request.id = data.id();
        request.status = BookingRequestStatus.valueOf(data.status());
        request.resourceId = ResourceId.of(UUID.fromString(data.resourceId()));
        request.startAt = data.startAt();
        request.durationMinutes = data.durationMinutes();
        request.endAt = data.endAt();
        request.customerName = data.customerName();
        request.contact = data.contact();
        request.note = data.note();
        request.linkId = data.linkId();
        request.submissionKey = data.submissionKey();
        request.advisoryFreeAtSubmission = data.advisoryFreeAtSubmission();
        request.createdAt = data.createdAt();
        request.updatedAt = data.updatedAt();
        request.decidedAt = data.decidedAt();
        request.decidedBy = data.decidedBy();
        request.reservationId = data.reservationId();
        return request;
    }

    BookingRequestDataValue toDataValue() {
        return new BookingRequestDataValue(id, status.name(), resourceId.value().toString(),
                startAt, durationMinutes, endAt, customerName, contact, note,
                linkId, submissionKey, advisoryFreeAtSubmission,
                createdAt, updatedAt, decidedAt, decidedBy, reservationId);
    }

    boolean belongsTo(UUID linkId) {
        return this.linkId.equals(linkId);
    }

    boolean isPending() {
        return status == BookingRequestStatus.PENDING;
    }

    void approve(Instant now, String decideBy, UUID reservationId) {
        requirePending();
        this.status = BookingRequestStatus.APPROVED;
        this.decidedAt = now;
        this.decidedBy = decideBy;
        this.reservationId = reservationId;
        this.updatedAt = now;
    }

    void reject(Instant now, String decideBy) {
        requirePending();
        this.status = BookingRequestStatus.REJECTED;
        this.decidedAt = now;
        this.decidedBy = decideBy;
        this.reservationId = null;
        this.updatedAt = now;
    }

    private void requirePending() {
        if (status != BookingRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_REQUEST_ALREADY_DECIDED);
        }
    }
}

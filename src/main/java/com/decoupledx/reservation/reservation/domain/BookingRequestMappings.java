package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestStatus;
import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestDataValue;
import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestLinkDataValue;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;

import java.util.List;
import java.util.UUID;

/**
 * View mappings of the booking-request workflow.
 */
final class BookingRequestMappings {

    private BookingRequestMappings() {
    }

    static BookingRequestInfo toInfo(BookingRequestDataValue data) {
        return new BookingRequestInfo(
                data.id(),
                referenceOf(data.id()),
                BookingRequestStatus.valueOf(data.status()),
                ResourceId.of(UUID.fromString(data.resourceId())),
                data.startAt(),
                data.durationMinutes(),
                data.endAt(),
                data.customerName(),
                data.contact(),
                data.note(),
                data.advisoryFreeAtSubmission(),
                data.createdAt(),
                data.decidedAt(),
                data.decidedBy(),
                data.reservationId());
    }

    /**
     * The coach-facing queue returns newest first regardless of status.
     */
    static List<BookingRequestInfo> toInfos(List<BookingRequestDataValue> data) {
        return data.stream().map(BookingRequestMappings::toInfo).toList();
    }

    static BookingRequestLinkInfo toLinkInfo(BookingRequestLinkDataValue data) {
        return new BookingRequestLinkInfo(data.id(), data.label(), data.createdAt(),
                data.revokedAt() != null);
    }

    private static String referenceOf(UUID id) {
        return "BKR-" + id.toString().substring(0, 8).toUpperCase();
    }
}

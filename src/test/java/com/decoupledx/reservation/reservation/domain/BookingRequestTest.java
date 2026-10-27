package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkToken;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestStatus;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingRequestTest {

    private static final Instant NOW = Instant.parse("2026-08-29T10:00:00Z");
    private static final LocalDateTime SLOT = LocalDateTime.of(2026, 9, 3, 18, 0);

    private BookingRequest newPendingRequest() {
        return BookingRequest.create(ResourceId.random(), SLOT, 90,
                "Ann Smith", "+48 600 100 200", "please", UUID.randomUUID(),
                "bkr-key", true, NOW);
    }

    @Test
    void startsPendingWithDerivedEndAndReference() {
        BookingRequest request = newPendingRequest();
        assertThat(request.getStatus()).isEqualTo(BookingRequestStatus.PENDING);
        assertThat(request.getEndAt()).isEqualTo(SLOT.plusMinutes(90));
        assertThat(request.isPending()).isTrue();
        assertThat(request.getCreatedAt()).isEqualTo(NOW);
        assertThat(BookingRequestMappings.toInfo(request.toDataValue()).reference())
                .matches("BKR-[0-9A-F]{8}");
    }

    @Test
    void approveLinksReservationAndDecides() {
        BookingRequest request = newPendingRequest();
        UUID reservationId = UUID.randomUUID();

        request.approve(Instant.parse("2026-08-30T09:00:00Z"), "actor-1", reservationId);

        assertThat(request.getStatus()).isEqualTo(BookingRequestStatus.APPROVED);
        assertThat(request.getReservationId()).isEqualTo(reservationId);
        assertThat(request.getDecidedAt()).isEqualTo(Instant.parse("2026-08-30T09:00:00Z"));
        assertThat(request.getDecidedBy()).isEqualTo("actor-1");
        assertThat(request.getUpdatedAt()).isEqualTo(request.getDecidedAt());
    }

    @Test
    void rejectLeavesNoReservationAttached() {
        BookingRequest request = newPendingRequest();
        request.reject(NOW, "actor-1");
        assertThat(request.getStatus()).isEqualTo(BookingRequestStatus.REJECTED);
        assertThat(request.getReservationId()).isNull();
    }

    @Test
    void decidedRequestsAreNoLongerDecidable_racingSafe() {
        BookingRequest request = newPendingRequest();
        request.reject(NOW, "actor-1");

        assertThatThrownBy(() -> request.approve(NOW, "actor-2", UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.BOOKING_REQUEST_ALREADY_DECIDED);
        assertThatThrownBy(() -> request.reject(NOW, "actor-2"))
                .isInstanceOf(BusinessException.class);
        // First decision wins.
        assertThat(request.getDecidedBy()).isEqualTo("actor-1");
    }

    @Test
    void tokenCarriesNoSequentialDataAndHashesStable() {
        BookingRequestLinkToken token = BookingRequestLinkToken.generate();

        assertThat(token.value()).matches(Pattern.compile("^[A-Za-z0-9_-]{22}$"));
        assertThat(token.hashedValue()).matches("^[0-9a-f]{64}$");
        // stable + injective for the same value
        assertThat(new BookingRequestLinkToken(token.value()).hashedValue())
                .isEqualTo(token.hashedValue());
        assertThat(new BookingRequestLinkToken(token.value() + "x").hashedValue())
                .isNotEqualTo(token.hashedValue());
    }
}

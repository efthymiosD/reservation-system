package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestLinkDataValue;

import java.util.Optional;

public interface BookingRequestLinkRepository {

    BookingRequestLinkDataValue save(BookingRequestLinkDataValue link);

    /**
     * Any state (active or revoked) — the caller decides what unknown/revoked means.
     */
    Optional<BookingRequestLinkDataValue> findByTokenHash(String tokenHash);

    /**
     * The at-most-one reusable link of the MVP.
     */
    Optional<BookingRequestLinkDataValue> findActive();
}

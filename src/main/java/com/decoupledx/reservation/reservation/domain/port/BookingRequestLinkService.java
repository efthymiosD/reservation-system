package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkToken;

import java.util.Optional;
import java.util.UUID;

public interface BookingRequestLinkService {

    /**
     * Public-form entry: the link id for a raw token, or the module's link
     * failures (unknown/revoked).
     */
    UUID resolveLinkId(String rawToken);

    Optional<BookingRequestLinkInfo> activeLink();

    /**
     * Issues a fresh active link and revokes the previous one. The raw token
     * exists ONLY in the returned value — it is displayed to the coach once
     * and never persisted.
     */
    BookingRequestLinkToken issueNewLink(String label);

    void revokeActive();
}

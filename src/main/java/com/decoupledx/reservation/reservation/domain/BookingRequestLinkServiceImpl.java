package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkToken;
import com.decoupledx.reservation.reservation.domain.port.BookingRequestLinkRepository;
import com.decoupledx.reservation.reservation.domain.port.BookingRequestLinkService;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * The shareable-link lifecycle. MVP: at most one active link; issuing a new
 * one revokes the previous (regeneration). Only the token's SHA-256 hash is
 * stored; the raw token leaves the module exactly once per issue.
 */
@Slf4j
@RequiredArgsConstructor
class BookingRequestLinkServiceImpl implements BookingRequestLinkService {

    private final BookingRequestLinkRepository links;
    private final Clock clock;
    private final TransactionRunner tx;

    @Override
    public UUID resolveLinkId(String rawToken) {
        return tx.run(() -> {
            if (rawToken == null || rawToken.isBlank()) {
                throw new BusinessException(ErrorCode.BOOKING_REQUEST_LINK_INVALID);
            }
            BookingRequestLink link = links.findByTokenHash(hashOf(rawToken))
                    .map(BookingRequestLink::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_REQUEST_LINK_INVALID));
            if (!link.isActive()) {
                throw new BusinessException(ErrorCode.BOOKING_REQUEST_LINK_REVOKED);
            }
            return link.getId();
        });
    }

    @Override
    public Optional<BookingRequestLinkInfo> activeLink() {
        return links.findActive().map(BookingRequestMappings::toLinkInfo);
    }

    @Override
    public BookingRequestLinkToken issueNewLink(String label) {
        return tx.run(() -> {
            // Regeneration semantics: the previous reusable link dies.
            links.findActive().ifPresent(current -> {
                BookingRequestLink revoked = BookingRequestLink.from(current);
                revoked.revoke(clock.instant());
                links.save(revoked.toDataValue());
            });
            BookingRequestLinkToken token = BookingRequestLinkToken.generate();
            BookingRequestLink issued = BookingRequestLink.issue(
                    label, token.hashedValue(), clock.instant());
            links.save(issued.toDataValue());
            log.info("Booking-request link issued id={} label={}", issued.getId(), issued.getLabel());
            return token;
        });
    }

    @Override
    public void revokeActive() {
        tx.run(() -> {
            links.findActive().ifPresent(current -> {
                BookingRequestLink link = BookingRequestLink.from(current);
                link.revoke(clock.instant());
                links.save(link.toDataValue());
                log.info("Booking-request link revoked id={}", link.getId());
            });
            return null;
        });
    }

    private static String hashOf(String rawToken) {
        return new BookingRequestLinkToken(rawToken).hashedValue();
    }
}

package com.decoupledx.reservation.reservation.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookingRequestLinkJpaRepository extends JpaRepository<BookingRequestLinkEntity, UUID> {

    Optional<BookingRequestLinkEntity> findByTokenHash(String tokenHash);

    Optional<BookingRequestLinkEntity> findByRevokedAtIsNull();
}

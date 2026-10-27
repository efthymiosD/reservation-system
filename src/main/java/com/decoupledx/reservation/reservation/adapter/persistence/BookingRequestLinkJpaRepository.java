package com.decoupledx.reservation.reservation.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BookingRequestLinkJpaRepository extends JpaRepository<BookingRequestLinkEntity, UUID> {

    Optional<BookingRequestLinkEntity> findByTokenHash(String tokenHash);

    Optional<BookingRequestLinkEntity> findByRevokedAtIsNull();
}

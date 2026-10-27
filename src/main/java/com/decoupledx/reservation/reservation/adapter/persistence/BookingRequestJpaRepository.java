package com.decoupledx.reservation.reservation.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BookingRequestJpaRepository extends JpaRepository<BookingRequestEntity, UUID> {

    Optional<BookingRequestEntity> findBySubmissionKey(String submissionKey);

    /**
     * Newest first; a null status means every state. Derived queries cannot
     * express "null means no filter", so the adapter branches between these.
     */
    Page<BookingRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BookingRequestEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);
}

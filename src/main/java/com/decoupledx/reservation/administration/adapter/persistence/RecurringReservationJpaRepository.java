package com.decoupledx.reservation.administration.adapter.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RecurringReservationJpaRepository extends JpaRepository<RecurringReservationEntity, UUID> {

    List<RecurringReservationEntity> findAllByOrderByCreatedAtDesc();

    List<RecurringReservationEntity> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Cursor-only advance, guarded on the previous cursor: a concurrent
     * cancel is honored (its status fields are untouched), a competing
     * materializer that already advanced is idempotently ignored, and the
     * stale domain snapshot cannot resurrect status/audit fields.
     */
    @Modifying
    @Query("""
            update RecurringReservationEntity r
            set r.nextOccurrence = :newCursor
            where r.id = :id and r.nextOccurrence < :newCursor
            """)
    int advanceCursor(@Param("id") UUID id, @Param("newCursor") LocalDate newCursor);
}
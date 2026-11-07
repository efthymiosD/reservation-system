package com.decoupledx.reservation.administration.domain.port;

import com.decoupledx.reservation.administration.adapter.persistence.RecurringReservationDataValue;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringReservationRepository {

    RecurringReservationDataValue save(RecurringReservationDataValue recurringReservation);

    /**
     * Advances the materialization cursor past the attempted window — the
     * ONLY write the materializer performs on the row. Conditional on the
     * previous cursor (`WHERE next_occurrence < ?`), so a concurrent cancel
     * (which never touches the cursor) is honored, a competing materializer
     * that already advanced is idempotently ignored, and no stale status/
     * cancellation audit fields can be resurrected from the domain snapshot.
     */
    void advanceCursor(UUID id, LocalDate newCursor);

    Optional<RecurringReservationDataValue> findById(UUID id);

    List<RecurringReservationDataValue> findAll();

    List<RecurringReservationDataValue> findAllActive();
}
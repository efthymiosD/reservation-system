package com.decoupledx.reservation.administration.adapter.persistence;

import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RecurringReservationDataRepo implements RecurringReservationRepository {

    private final RecurringReservationJpaRepository recurringReservations;
    private final RecurringReservationDataConverter dataConverter;
    private final TransactionRunner tx;

    @Override
    public RecurringReservationDataValue save(RecurringReservationDataValue recurringReservation) {
        // Self-contained: does NOT rely on a caller-provided shared
        // persistence context (the materializer no longer wraps the batch in
        // one tx — 2026-11-07 aborted-transaction rule). findById here and
        // saveAndFlush below are separate units, so the detached update is
        // MERGED inside saveAndFlush's own transaction.
        RecurringReservationEntity existing = recurringReservations
                .findById(recurringReservation.id()).orElse(null);
        if (existing != null) {
            existing.updateFrom(
                    recurringReservation.status().name(),
                    recurringReservation.nextOccurrence(),
                    recurringReservation.cancelledAt(),
                    recurringReservation.cancelledBy() == null ? null : recurringReservation.cancelledBy().value());
            recurringReservations.saveAndFlush(existing);
        } else {
            recurringReservations.saveAndFlush(dataConverter.toEntity(recurringReservation));
        }
        return recurringReservation;
    }

    @Override
    public Optional<RecurringReservationDataValue> findById(UUID id) {
        return recurringReservations.findById(id).map(dataConverter::toDataValue);
    }

    @Override
    public void advanceCursor(UUID id, LocalDate newCursor) {
        // One self-contained unit — @Modifying queries are not auto-transactional.
        tx.run(() -> recurringReservations.advanceCursor(id, newCursor));
    }

    @Override
    public List<RecurringReservationDataValue> findAll() {
        return recurringReservations.findAllByOrderByCreatedAtDesc().stream()
                .map(dataConverter::toDataValue)
                .toList();
    }

    @Override
    public List<RecurringReservationDataValue> findAllActive() {
        return recurringReservations.findByStatusOrderByCreatedAtDesc(RecurringReservationStatus.ACTIVE.name()).stream()
                .map(dataConverter::toDataValue)
                .toList();
    }

}
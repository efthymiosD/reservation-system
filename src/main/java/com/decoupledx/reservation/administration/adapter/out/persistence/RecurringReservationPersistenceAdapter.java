package com.decoupledx.reservation.administration.adapter.out.persistence;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.administration.domain.model.RecurringReservation;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RecurringReservationPersistenceAdapter implements RecurringReservationRepository {

    private final RecurringReservationJpaRepository recurringReservations;

    @Override
    public RecurringReservation save(RecurringReservation recurringReservation) {
        recurringReservations.findById(recurringReservation.getId())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(
                                recurringReservation.getStatus().name(),
                                recurringReservation.getNextOccurrence(),
                                recurringReservation.getCancelledAt(),
                                recurringReservation.getCancelledBy() == null ? null : recurringReservation.getCancelledBy().value()),
                        () -> recurringReservations.saveAndFlush(toEntity(recurringReservation)));
        recurringReservations.flush();
        return recurringReservation;
    }

    @Override
    public Optional<RecurringReservation> findById(UUID id) {
        return recurringReservations.findById(id).map(this::toDomain);
    }

    @Override
    public List<RecurringReservation> findAll() {
        return recurringReservations.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RecurringReservation> findAllActive() {
        return recurringReservations.findByStatusOrderByCreatedAtDesc(RecurringReservationStatus.ACTIVE.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    private RecurringReservationEntity toEntity(RecurringReservation recurringReservation) {
        return new RecurringReservationEntity(
                recurringReservation.getId(),
                recurringReservation.getResourceId(),
                UUID.fromString(recurringReservation.getCustomerId().value()),
                recurringReservation.getWeekday().name(),
                recurringReservation.getStartTime(),
                recurringReservation.getEndTime(),
                recurringReservation.getWindowMonths(),
                recurringReservation.getStatus().name(),
                recurringReservation.getNextOccurrence(),
                recurringReservation.getCreatedAt(),
                recurringReservation.getCancelledAt(),
                recurringReservation.getCancelledBy() == null ? null : recurringReservation.getCancelledBy().value());
    }

    private RecurringReservation toDomain(RecurringReservationEntity entity) {
        return RecurringReservation.reconstitute(
                entity.getId(),
                entity.getResourceId(),
                CustomerId.of(entity.getCustomerId().toString()),
                DayOfWeek.valueOf(entity.getWeekday()),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getWindowMonths(),
                RecurringReservationStatus.valueOf(entity.getStatus()),
                entity.getNextOccurrence(),
                entity.getCreatedAt(),
                entity.getCancelledAt(),
                entity.getCancelledBy() == null ? null : CustomerId.of(entity.getCancelledBy()));
    }
}
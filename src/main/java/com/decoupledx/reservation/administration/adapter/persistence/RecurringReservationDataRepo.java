package com.decoupledx.reservation.administration.adapter.persistence;

import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RecurringReservationDataRepo implements RecurringReservationRepository {

    private final RecurringReservationJpaRepository recurringReservations;
    private final RecurringReservationDataConverter dataConverter;

    @Override
    public RecurringReservationDataValue save(RecurringReservationDataValue recurringReservation) {
        recurringReservations.findById(recurringReservation.id())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(
                                recurringReservation.status().name(),
                                recurringReservation.nextOccurrence(),
                                recurringReservation.cancelledAt(),
                                recurringReservation.cancelledBy() == null ? null : recurringReservation.cancelledBy().value()),
                        () -> recurringReservations.saveAndFlush(dataConverter.toEntity(recurringReservation)));
        recurringReservations.flush();
        return recurringReservation;
    }

    @Override
    public Optional<RecurringReservationDataValue> findById(UUID id) {
        return recurringReservations.findById(id).map(dataConverter::toDataValue);
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
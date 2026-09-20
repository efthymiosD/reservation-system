package com.decoupledx.reservation.administration.adapter.persistence;

import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.UUID;

@Component
class RecurringReservationDataConverter {

    public RecurringReservationEntity toEntity(RecurringReservationDataValue recurringReservation) {
        return new RecurringReservationEntity(
                recurringReservation.id(),
                recurringReservation.resourceId(),
                UUID.fromString(recurringReservation.customerId().value()),
                recurringReservation.weekday().name(),
                recurringReservation.startTime(),
                recurringReservation.endTime(),
                recurringReservation.windowMonths(),
                recurringReservation.status().name(),
                recurringReservation.nextOccurrence(),
                recurringReservation.createdAt(),
                recurringReservation.cancelledAt(),
                recurringReservation.cancelledBy() == null ? null : recurringReservation.cancelledBy().value());
    }

    public RecurringReservationDataValue toDataValue(RecurringReservationEntity entity) {
        return RecurringReservationDataValue.builder()
                .id(entity.getId())
                .resourceId(entity.getResourceId())
                .customerId(CustomerId.of(entity.getCustomerId().toString()))
                .weekday(DayOfWeek.valueOf(entity.getWeekday()))
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .windowMonths(entity.getWindowMonths())
                .status(RecurringReservationStatus.valueOf(entity.getStatus()))
                .nextOccurrence(entity.getNextOccurrence())
                .createdAt(entity.getCreatedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancelledBy(entity.getCancelledBy() == null ? null : CustomerId.of(entity.getCancelledBy()))
                .build();
    }
}
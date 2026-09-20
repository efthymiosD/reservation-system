package com.decoupledx.reservation.administration.domain;

import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.administration.adapter.persistence.RecurringReservationDataValue;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import lombok.Getter;

import java.time.*;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root for a recurring per-customer reservation: a resource assigned
 * to a customer on a weekly weekday at venue-local times. The materializer
 * advances {@code nextOccurrence} (the cursor) past each occurrence it turns
 * into a real reservation, so each week's slot is created exactly once.
 */
@Getter
class RecurringReservation {

    private final UUID id;
    private final UUID resourceId;
    private final CustomerId customerId;
    private final DayOfWeek weekday;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int windowMonths;
    private RecurringReservationStatus status;
    private LocalDate nextOccurrence;
    private final Instant createdAt;
    private Instant cancelledAt;
    private CustomerId cancelledBy;

    private RecurringReservation(UUID id, UUID resourceId, CustomerId customerId, DayOfWeek weekday,
                                 LocalTime startTime, LocalTime endTime, int windowMonths,
                                 RecurringReservationStatus status,
                                 LocalDate nextOccurrence, Instant createdAt, Instant cancelledAt,
                                 CustomerId cancelledBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.customerId = customerId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
        this.windowMonths = windowMonths;
        this.status = status;
        this.nextOccurrence = nextOccurrence;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }

    public static RecurringReservation create(UUID resourceId, CustomerId customerId, DayOfWeek weekday,
                                              LocalTime startTime, LocalTime endTime, int windowMonths,
                                              LocalDate nextOccurrence, Instant now) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(weekday, "weekday must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        Objects.requireNonNull(nextOccurrence, "nextOccurrence must not be null");
        Objects.requireNonNull(now, "now must not be null");
        requireValidWindow(windowMonths);
        return new RecurringReservation(
                UUID.randomUUID(), resourceId, customerId, weekday, startTime, endTime, windowMonths,
                RecurringReservationStatus.ACTIVE, nextOccurrence, now, null, null);
    }

    /**
     * Cancels the whole recurring reservation. The materializer will skip it from now on;
     * cancelling already-materialized future reservations is the caller's job
     * (the admin cancel use case does it in the same transaction).
     */
    public void cancel(Instant now, CustomerId actor) {
        requireActive();
        this.status = RecurringReservationStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = actor;
    }

    /**
     * Advances the materializer cursor so an occurrence is never revisited.
     */
    public void advanceCursor(LocalDate newCursor) {
        if (newCursor.isAfter(this.nextOccurrence)) {
            this.nextOccurrence = newCursor;
        }
    }

    public boolean isActive() {
        return status == RecurringReservationStatus.ACTIVE;
    }

    public int durationMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    private void requireActive() {
        if (status == RecurringReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.RECURRING_RESERVATION_ALREADY_CANCELLED);
        }
    }

    private static void requireValidWindow(int windowMonths) {
        if (windowMonths != 1 && windowMonths != 3 && windowMonths != 6) {
            throw new BusinessException(ErrorCode.INVALID_RECURRING_RESERVATION_WINDOW);
        }
    }

    public static RecurringReservation reconstitute(RecurringReservationDataValue data) {
        return new RecurringReservation(
                data.id(), data.resourceId(), data.customerId(), data.weekday(), data.startTime(), data.endTime(),
                data.windowMonths(), data.status(), data.nextOccurrence(), data.createdAt(), data.cancelledAt(), data.cancelledBy());
    }

    public RecurringReservationDataValue toDataValue() {
        return RecurringReservationDataValue.builder()
                .id(id)
                .resourceId(resourceId)
                .customerId(customerId)
                .weekday(weekday)
                .startTime(startTime)
                .endTime(endTime)
                .windowMonths(windowMonths)
                .status(status)
                .nextOccurrence(nextOccurrence)
                .createdAt(createdAt)
                .cancelledAt(cancelledAt)
                .cancelledBy(cancelledBy)
                .build();
    }

    public RecurringReservationInfo toInfo() {
        return RecurringReservationInfo.builder()
                .id(id)
                .resourceId(resourceId)
                .customerId(UUID.fromString(customerId.value()))
                .weekday(weekday)
                .startTime(startTime)
                .endTime(endTime)
                .windowMonths(windowMonths)
                .status(status)
                .nextOccurrence(nextOccurrence)
                .createdAt(createdAt)
                .cancelledAt(cancelledAt)
                .cancelledBy(cancelledBy == null ? null : UUID.fromString(cancelledBy.value()))
                .build();
    }
}
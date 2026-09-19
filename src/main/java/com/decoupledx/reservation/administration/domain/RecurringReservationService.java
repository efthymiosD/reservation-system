package com.decoupledx.reservation.administration.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.administration.adapter.api.CreateRecurringReservationCommand;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.administration.domain.model.RecurringReservation;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;

import lombok.RequiredArgsConstructor;

/**
 * Create/cancel/query use cases for recurring reservations. Creation validates
 * the slot definition up front (active resource, period sanity, opening-hours
 * fit and duration policy for the next occurrence) and materializes the first
 * batch of occurrences in the same transaction.
 */
@RequiredArgsConstructor
public class RecurringReservationService {

    private final RecurringReservationRepository recurringReservations;
    private final ResourceApi resourceService;
    private final VenueApi venueService;
    private final PolicyApi policyService;
    private final ReservationApi reservationApi;
    private final RecurringReservationMaterializationService materializer;
    private final Clock clock;
    private final TransactionRunner tx;

    public RecurringReservationInfo create(CreateRecurringReservationCommand command) {
        return tx.run(() -> {
            ResourceInfo resource = resourceService.lockResource(command.resourceId());
            if (!resource.isActive()) {
                throw new BusinessException(ErrorCode.RESOURCE_INACTIVE);
            }
            VenueInfo venue = venueService.getVenue(resource.venueId().value());
            validateDefinition(command, venue);

            LocalDate today = clock.instant().atZone(venue.timezone()).toLocalDate();
            RecurringReservation recurringReservation = RecurringReservation.create(
                    command.resourceId(),
                    CustomerId.of(command.customerId().toString()),
                    command.weekday(),
                    command.startTime(),
                    command.endTime(),
                    command.windowMonths(),
                    today,
                    clock.instant());
            RecurringReservationInfo saved = toInfo(recurringReservations.save(recurringReservation));
            materializer.materializeFor(saved.id());
            return saved;
        });
    }

    /**
     * Cancels the whole recurring reservation and every not-yet-started reservation it has
     * produced, all in one transaction.
     */
    public void cancel(UUID recurringReservationId, CustomerId actor) {
        tx.run(() -> {
            RecurringReservation recurringReservation = loadActive(recurringReservationId);
            Instant now = clock.instant();
            reservationApi.findActiveByRecurringReservation(recurringReservationId).stream()
                    .filter(reservation -> reservation.end().isAfter(now))
                    .forEach(reservation -> reservationApi.cancelAdministratively(reservation.id().value(), actor));
            recurringReservation.cancel(now, actor);
            recurringReservations.save(recurringReservation);
        });
    }

    public List<RecurringReservationInfo> findAll() {
        return recurringReservations.findAll().stream().map(this::toInfo).toList();
    }

    void validateDefinition(CreateRecurringReservationCommand command, VenueInfo venue) {
        if (command.endTime().isBefore(command.startTime()) || command.endTime().equals(command.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_RECURRING_RESERVATION_PERIOD);
        }
        ZoneId zone = venue.timezone();
        Duration duration = Duration.between(command.startTime(), command.endTime());
        BookingPolicy policy = policyService.bookingPolicyFor(venue.id().value());
        policy.validateDuration(duration);

        LocalDate today = clock.instant().atZone(zone).toLocalDate();
        LocalDate next = nextWeekdayOnOrAfter(today, command.weekday());
        ReservationPeriod sample = ReservationPeriod.of(
                next.atTime(command.startTime()).atZone(zone).toInstant(),
                next.atTime(command.endTime()).atZone(zone).toInstant());
        if (!venue.openingHours().fits(sample, zone)) {
            throw new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        LocalTime opensAt = venue.openingHours().opensAt(command.weekday())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS));
        policy.validateStartTime(sample.start().atZone(zone), opensAt);
    }

    private LocalDate nextWeekdayOnOrAfter(LocalDate date, java.time.DayOfWeek weekday) {
        LocalDate candidate = date;
        while (candidate.getDayOfWeek() != weekday) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private RecurringReservation loadActive(UUID recurringReservationId) {
        RecurringReservation recurringReservation = recurringReservations.findById(recurringReservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRING_RESERVATION_NOT_FOUND));
        if (!recurringReservation.isActive()) {
            throw new BusinessException(ErrorCode.RECURRING_RESERVATION_ALREADY_CANCELLED);
        }
        return recurringReservation;
    }

    private RecurringReservationInfo toInfo(RecurringReservation recurringReservation) {
        return new RecurringReservationInfo(
                recurringReservation.getId(),
                recurringReservation.getResourceId(),
                UUID.fromString(recurringReservation.getCustomerId().value()),
                recurringReservation.getWeekday(),
                recurringReservation.getStartTime(),
                recurringReservation.getEndTime(),
                recurringReservation.getWindowMonths(),
                recurringReservation.getStatus(),
                recurringReservation.getNextOccurrence(),
                recurringReservation.getCreatedAt(),
                recurringReservation.getCancelledAt(),
                recurringReservation.getCancelledBy() == null ? null : UUID.fromString(recurringReservation.getCancelledBy().value()));
    }
}
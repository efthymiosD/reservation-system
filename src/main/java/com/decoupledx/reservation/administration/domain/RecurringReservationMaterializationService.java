package com.decoupledx.reservation.administration.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import com.decoupledx.reservation.administration.adapter.api.RecurringReservationMaterializationSummary;
import com.decoupledx.reservation.administration.domain.model.RecurringReservation;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.time.Duration.*;

/**
 * Turns each active recurring reservation's upcoming occurrences (inside its
 * booking window of 1/3/6 months) into real reservations. Never rewinds a
 * cursor: each week's slot is attempted exactly once. Occurrences that conflict
 * with an existing reservation (customer overlap, slot taken) are skipped and
 * the cursor moves past them.
 */
@Slf4j
@RequiredArgsConstructor
public class RecurringReservationMaterializationService {

    private final RecurringReservationRepository recurringReservations;
    private final ReservationApi reservationApi;
    private final VenueApi venueApi;
    private final Clock clock;
    private final TransactionRunner tx;

    /**
     * Materializes every active recurring reservation up to its booking window.
     */
    public RecurringReservationMaterializationSummary materializeDue() {
        return tx.run(() -> {
            int created = 0;
            int skipped = 0;
            for (RecurringReservation recurringReservation : recurringReservations.findAllActive()) {
                RecurringReservationMaterializationSummary result = materialize(recurringReservation);
                created += result.created();
                skipped += result.skipped();
            }
            return new RecurringReservationMaterializationSummary(created, skipped);
        });
    }

    /**
     * Materializes a single recurringReservation (called right after creation so the
     * customer sees upcoming reservations immediately). Runs inside the current
     * transaction when invoked from another service.
     */
    public void materializeFor(UUID recurringReservationId) {
        tx.run(() -> {
            RecurringReservation recurringReservation = recurringReservations.findById(recurringReservationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RECURRING_RESERVATION_NOT_FOUND));
            return materialize(recurringReservation);
        });
    }

    private RecurringReservationMaterializationSummary materialize(RecurringReservation recurringReservation) {
        if (!recurringReservation.isActive()) {
            return new RecurringReservationMaterializationSummary(0, 0);
        }
        VenueInfo venue = venueApi.getVenue(venueApi.singleVenueId());
        ZoneId zone = venue.timezone();
        Instant now = clock.instant();
        LocalDate today = now.atZone(zone).toLocalDate();
        LocalDate latestDate = today.plusMonths(recurringReservation.getWindowMonths());

        int created = 0;
        int skipped = 0;
        LocalDate cursor = recurringReservation.getNextOccurrence();
        if (cursor.isBefore(today)) {
            cursor = today;
        }
        while (!cursor.isAfter(latestDate)) {
            if (cursor.getDayOfWeek() == recurringReservation.getWeekday()) {
                LocalDateTime occurrence = cursor.atTime(recurringReservation.getStartTime());
                if (occurrence.atZone(zone).toInstant().isAfter(now)) {
                    if (conflictsWithExistingBooking(recurringReservation.getResourceId(), recurringReservation, occurrence, zone)) {
                        skipped++;
                    } else {
                        try {
                            reservationApi.createForRecurringReservation(
                                    recurringReservation.getResourceId(), occurrence, recurringReservation.durationMinutes(),
                                    recurringReservation.getCustomerId(), recurringReservation.getId());
                            created++;
                        } catch (BusinessException conflict) {
                            // Last resort for a genuine concurrent race: the slot was taken
                            // between our overlap check and the insert.
                            log.warn("Materialization skipped recurringReservationId={} occurrence={} reason={}",
                                    recurringReservation.getId(), occurrence, conflict.getMessage());
                            skipped++;
                        }
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }
        recurringReservation.advanceCursor(latestDate.plusDays(1));
        recurringReservations.save(recurringReservation);
        if (created > 0 || skipped > 0) {
            log.info("Materialization recurringReservationId={} created={} skipped={}",
                    recurringReservation.getId(), created, skipped);
        }
        return new RecurringReservationMaterializationSummary(created, skipped);
    }

    private boolean conflictsWithExistingBooking(UUID resourceId, RecurringReservation recurringReservation,
            LocalDateTime occurrence, ZoneId zone) {
        Instant start = occurrence.atZone(zone).toInstant();
        Instant end = start.plus(ofMinutes(recurringReservation.durationMinutes()));
        ReservationPeriod period = ReservationPeriod.of(start, end);
        return !reservationApi.findActiveOverlappingResource(resourceId, period).isEmpty()
                || !reservationApi.findActiveOverlappingCustomer(recurringReservation.getCustomerId(), period).isEmpty();
    }
}
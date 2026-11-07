package com.decoupledx.reservation.administration.domain;

import static java.time.Duration.ofMinutes;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationMaterializationSummary;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationMaterializationService;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationRepository;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import java.time.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns each active recurring reservation's upcoming occurrences (inside its
 * booking window of 1/3/6 months) into real reservations. Never rewinds a
 * cursor: each week's slot is attempted exactly once. Occurrences that
 * conflict with an existing reservation (customer overlap, slot taken) are
 * skipped and the cursor moves past them.
 *
 * <p><b>Transaction units (2026-11-07 Postgres aborted-transaction rule):</b>
 * there is deliberately NO wrapping transaction over the batch. Each
 * occurrence's <code>create</code> runs in its own committed unit (with its
 * own exclusion-constraint races translated to 409), and the cursor advance
 * runs in its own unit afterwards — a failed/skipped attempt can abort its
 * own unit, but the failed statements can no longer destroy the run or
 * wedge the nightly job (the pre-fix catch-and-continue relied on stopping
 * after a REQUIRED-joined tx was already poisoned).
 */
@Slf4j
@RequiredArgsConstructor
class RecurringReservationMaterializationServiceImpl implements RecurringReservationMaterializationService {

    private final RecurringReservationRepository recurringReservations;
    private final ReservationApi reservationApi;
    private final VenueApi venueApi;
    private final Clock clock;

    @Override
    public RecurringReservationMaterializationSummary materializeDue() {
        // Each reservation is isolated: one reservation's failure (or its
        // cursor state) must never affect the others' materialization.
        int created = 0;
        int skipped = 0;
        for (var recurringReservation : recurringReservations.findAllActive().stream()
                .map(RecurringReservation::reconstitute).toList()) {
            try {
                RecurringReservationMaterializationSummary result = materialize(recurringReservation);
                created += result.created();
                skipped += result.skipped();
            } catch (RuntimeException failure) {
                log.error("Materialization failed recurringReservationId={}", recurringReservation.getId(),
                        failure);
            }
        }
        return new RecurringReservationMaterializationSummary(created, skipped);
    }

    @Override
    public void materializeFor(UUID recurringReservationId) {
        RecurringReservation recurringReservation = recurringReservations.findById(recurringReservationId)
                .map(RecurringReservation::reconstitute)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRING_RESERVATION_NOT_FOUND));
        materialize(recurringReservation);
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
                        // The create runs as its OWN transaction: a race or a
                        // policy failure rolls back that single attempt and the
                        // catch is free to keep going (each further statement —
                        // incl. the cursor advance below — starts a fresh unit).
                        try {
                            reservationApi.createForRecurringReservation(
                                    recurringReservation.getResourceId(), occurrence, recurringReservation.durationMinutes(),
                                    recurringReservation.getCustomerId(), recurringReservation.getId());
                            created++;
                        } catch (BusinessException conflict) {
                            // Last resort for a genuine concurrent race: the slot was taken
                            // between our overlap check and the insert; also a policy
                            // rejection (e.g. DST spring-forward outside opening hours).
                            log.warn("Materialization skipped recurringReservationId={} occurrence={} reason={}",
                                    recurringReservation.getId(), occurrence, conflict.getMessage());
                            skipped++;
                        }
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }
        // Own unit, guarded on the previous cursor: committed even after
        // skipped/failed attempts (the job never wedges), a concurrent cancel
        // is honored (the guarded update touches ONLY the cursor), and a
        // competing materializer that already advanced is a no-op.
        recurringReservations.advanceCursor(recurringReservation.getId(),
                latestDate.plusDays(1));
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

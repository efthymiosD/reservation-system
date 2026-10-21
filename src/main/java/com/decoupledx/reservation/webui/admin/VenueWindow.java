package com.decoupledx.reservation.webui.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * The venue-local date window of the admin reservations listing, interpreted
 * as start-in-range instants and clipped to the configured maximum width in
 * days. The form echo still carries what was typed; {@code clamped} signals
 * that the PAGE window was trimmed so the UI can say so. Both bounds stay
 * open (null) when no dates were given. {@code from} is inclusive,
 * {@code to} inclusive, with {@code from = to} meaning that single day.
 */
record VenueWindow(ZoneId zone, LocalDate from, LocalDate to, boolean clamped) {

    static VenueWindow forRange(ZoneId zone, LocalDate rawFrom, LocalDate rawTo, int maxDays) {
        LocalDate from = rawFrom;
        LocalDate to = rawTo;
        boolean clamped = false;
        if (from == null && to != null) {
            from = to.minusDays((long) maxDays - 1);
            clamped = true;
        } else if (from != null && to == null) {
            to = from.plusDays((long) maxDays - 1);
            clamped = true;
        } else if (spanDays(from, to) > maxDays) {
            to = from.plusDays((long) maxDays - 1);
            clamped = true;
        }
        return new VenueWindow(zone, from, to, clamped);
    }

    /** {code null} when the window stays open (no dates given). */
    Instant startInclusiveInstant() {
        return from == null ? null : from.atStartOfDay(zone).toInstant();
    }

    Instant endExclusiveInstant() {
        return to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();
    }

    private static long spanDays(LocalDate from, LocalDate to) {
        return from == null || to == null ? -1 : ChronoUnit.DAYS.between(from, to) + 1;
    }
}

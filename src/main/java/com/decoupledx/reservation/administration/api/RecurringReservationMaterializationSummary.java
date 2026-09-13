package com.decoupledx.reservation.administration.api;

/**
 * Outcome of a materialization run: how many upcoming occurrences were turned
 * into real reservations and how many were skipped (slot taken by a conflicting
 * reservation / rule violation).
 */
public record RecurringReservationMaterializationSummary(int created, int skipped) {

    public RecurringReservationMaterializationSummary {
        if (created < 0 || skipped < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
    }
}
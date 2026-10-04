package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inbound port: booking use case. The start time is venue-local wall-clock time;
 * the service owns timezone and period arithmetic.
 */
public interface CreateReservationService {

    /**
     * Customer-facing entry point for the current player (ADR 0002): the
     * customer is resolved inside the use case from the session context.
     */
    ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes);

    ReservationInfo create(UUID resourceId, LocalDateTime startTime, int durationMinutes, CustomerId customer);

    /**
     * Entry point for the recurring-reservation materializer: identical to {@link #create}
     * but links the created reservation to the recurring reservation that produced it.
     */
    void createForRecurringReservation(UUID resourceId, LocalDateTime startTime, int durationMinutes,
                                       CustomerId customer, UUID recurringReservationId);
}

package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * The reservation-period rules in one place — the checks reservation creation
 * performs — shared with the booking-request submit path so no rule is
 * written twice (availability and overlap checks stay with the authoritative
 * creation path; this validates a period against the venue's own policy and
 * opening hours). Plain bean, wired in {@link ReservationDomainConfig} — the
 * domain stays framework-free.
 */
class SlotPeriodRules {

    private final PolicyApi policyService;
    private final Clock clock;

    SlotPeriodRules(PolicyApi policyService, Clock clock) {
        this.policyService = policyService;
        this.clock = clock;
    }

    /**
     * @param forRecurringMaterialization recurring bookings are governed by
     *                                    their own booking window, not the venue's public advance cap.
     */
    void check(Instant start, ReservationPeriod period, VenueInfo venue,
               boolean forRecurringMaterialization) {
        ZoneId zone = venue.timezone();
        BookingPolicy bookingPolicy = policyService.bookingPolicyFor(venue.id().value());
        bookingPolicy.validateDuration(period.duration());

        OpeningHours openingHours = venue.openingHours();
        if (!openingHours.fits(period, zone)) {
            throw new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        LocalTime opensAt = openingHours
                .opensAt(start.atZone(zone).getDayOfWeek())
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTSIDE_OPENING_HOURS));
        bookingPolicy.validateStartTime(start.atZone(zone), opensAt);
        bookingPolicy.validateNotInPast(clock.instant(), start);
        if (!forRecurringMaterialization) {
            bookingPolicy.validateAdvanceBooking(clock.instant(), start, zone);
        }
    }
}

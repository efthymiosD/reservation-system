package com.decoupledx.reservation.reservation.adapter.in.web;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;

import lombok.RequiredArgsConstructor;

/**
 * Builds customer-facing reservation views. Owns the lookup of the current
 * cancellation policy and the canCancel UX hint (the rule itself lives in
 * {@link ReservationInfo#isCancellable}); controllers stay pure delegation.
 */
@Component
@RequiredArgsConstructor
class ReservationViews {

    private final Clock clock;
    private final PolicyApi policyService;
    private final VenueApi venueService;

    ReservationResponse from(ReservationInfo reservation) {
        return from(reservation, cancellationPolicy());
    }

    MyReservationsPage pageFrom(ReservationQueryService.ReservationPage result) {
        CancellationPolicy cancellationPolicy = cancellationPolicy();
        List<ReservationResponse> items = result.items().stream()
                .map(reservation -> from(reservation, cancellationPolicy))
                .toList();
        return new MyReservationsPage(items, result.total(), result.page(), result.size());
    }

    private ReservationResponse from(ReservationInfo reservation, CancellationPolicy cancellationPolicy) {
        return new ReservationResponse(
                reservation.id().value(),
                reservation.resourceId().value(),
                reservation.start(),
                reservation.end(),
                reservation.status().name(),
                reservation.price().amount(),
                reservation.price().currency().getCurrencyCode(),
                reservation.createdAt(),
                reservation.cancelledAt(),
                reservation.isCancellable(clock.instant(), cancellationPolicy.deadlineBeforeStart()));
    }

    private CancellationPolicy cancellationPolicy() {
        return policyService.cancellationPolicyFor(venueService.singleVenueId());
    }
}

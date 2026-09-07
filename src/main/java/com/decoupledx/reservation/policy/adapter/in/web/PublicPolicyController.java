package com.decoupledx.reservation.policy.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.policy.domain.model.BookingPolicy;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import lombok.RequiredArgsConstructor;

/**
 * Public, read-only booking policy so clients can build date/time/duration controls
 * from backend configuration instead of hardcoding business rules.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
class PublicPolicyController {

    private final PolicyService policyService;
    private final VenueService venueService;

    @GetMapping("/booking-policy")
    BookingPolicyPublicResponse bookingPolicy() {
        BookingPolicy policy = policyService.getBookingPolicy(venueService.singleVenueId());
        return new BookingPolicyPublicResponse(
                policy.minDuration().toMinutes(),
                policy.maxDuration().toMinutes(),
                policy.durationStep().toMinutes(),
                policy.startTimeStep().toMinutes(),
                policy.maxAdvanceBooking().toString());
    }

    record BookingPolicyPublicResponse(
            long minDurationMinutes,
            long maxDurationMinutes,
            long durationStepMinutes,
            long startTimeStepMinutes,
            String maxAdvanceBooking) {
    }
}

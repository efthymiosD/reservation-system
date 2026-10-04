package com.decoupledx.reservation.policy.adapter.web;

import com.decoupledx.reservation.policy.adapter.api.BookingPolicy;
import com.decoupledx.reservation.policy.adapter.api.CancellationPolicy;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class PolicyAdminController {

    private final PolicyApi policyService;
    private final VenueApi venueService;

    @GetMapping("/booking-policy")
    BookingPolicyGetResponse getBookingPolicy() {
        BookingPolicy policy = policyService.bookingPolicyFor(venueService.singleVenueId());
        return new BookingPolicyGetResponse(
                policy.minDuration().toMinutes(),
                policy.maxDuration().toMinutes(),
                policy.durationStep().toMinutes(),
                policy.startTimeStep().toMinutes(),
                policy.maxAdvanceBooking().toString());
    }

    @GetMapping("/cancellation-policy")
    CancellationPolicyGetResponse getCancellationPolicy() {
        CancellationPolicy policy = policyService.cancellationPolicyFor(venueService.singleVenueId());
        return new CancellationPolicyGetResponse(policy.deadlineBeforeStart().toMinutes());
    }

    @PutMapping("/booking-policy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateBookingPolicy(@Valid @RequestBody BookingPolicyUpdateRequest request) {
        BookingPolicy policy = new BookingPolicy(
                Duration.ofMinutes(request.minDurationMinutes()),
                Duration.ofMinutes(request.maxDurationMinutes()),
                Duration.ofMinutes(request.durationStepMinutes()),
                Duration.ofMinutes(request.startTimeStepMinutes()),
                parseAdvanceBooking(request.maxAdvanceBooking()));
        policyService.updateBookingPolicy(venueService.singleVenueId(), policy);
    }

    @PutMapping("/cancellation-policy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateCancellationPolicy(@Valid @RequestBody CancellationPolicyUpdateRequest request) {
        CancellationPolicy policy =
                new CancellationPolicy(Duration.ofMinutes(request.deadlineBeforeStartMinutes()));
        policyService.updateCancellationPolicy(venueService.singleVenueId(), policy);
    }

    private static Period parseAdvanceBooking(String iso) {
        try {
            return Period.parse(iso);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_POLICY,
                    "maxAdvanceBooking must be an ISO-8601 period such as P1M");
        }
    }

}

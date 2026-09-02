package com.decoupledx.reservation.policy.adapter.in.web;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.policy.domain.model.BookingPolicy;
import com.decoupledx.reservation.policy.domain.model.CancellationPolicy;
import com.decoupledx.reservation.policy.domain.service.PolicyService;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class PolicyAdminController {

    private final PolicyService policyService;
    private final VenueService venueService;

    @GetMapping("/booking-policy")
    BookingPolicyGetResponse getBookingPolicy() {
        BookingPolicy policy = policyService.getBookingPolicy(venueService.singleVenueId());
        return new BookingPolicyGetResponse(
                policy.minDuration().toMinutes(),
                policy.maxDuration().toMinutes(),
                policy.durationStep().toMinutes(),
                policy.startTimeStep().toMinutes(),
                policy.maxAdvanceBooking().toString());
    }

    @GetMapping("/cancellation-policy")
    CancellationPolicyGetResponse getCancellationPolicy() {
        CancellationPolicy policy = policyService.getCancellationPolicy(venueService.singleVenueId());
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

    record BookingPolicyUpdateRequest(
            @Positive int minDurationMinutes,
            @Positive int maxDurationMinutes,
            @Positive int durationStepMinutes,
            @Positive int startTimeStepMinutes,
            @NotBlank String maxAdvanceBooking) {
    }

    record CancellationPolicyUpdateRequest(
            @NotNull @PositiveOrZero Integer deadlineBeforeStartMinutes) {
    }

    record BookingPolicyGetResponse(
            long minDurationMinutes,
            long maxDurationMinutes,
            long durationStepMinutes,
            long startTimeStepMinutes,
            String maxAdvanceBooking) {
    }

    record CancellationPolicyGetResponse(long deadlineBeforeStartMinutes) {
    }
}

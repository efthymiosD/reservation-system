package com.decoupledx.reservation.administration.adapter.web;

import com.decoupledx.reservation.administration.adapter.api.CreateRecurringReservationCommand;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationMaterializationSummary;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationMaterializationService;
import com.decoupledx.reservation.administration.domain.port.RecurringReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/recurring-reservations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class RecurringReservationController {

    private final RecurringReservationService recurringReservationService;
    private final RecurringReservationMaterializationService materializationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RecurringReservationResponse create(@Valid @RequestBody CreateRecurringReservationRequest request) {
        return toResponse(recurringReservationService.create(toCommand(request)));
    }

    @GetMapping
    List<RecurringReservationResponse> findRecurringReservations(
            @RequestParam(required = false) RecurringReservationStatus status) {
        return recurringReservationService.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{recurringReservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID recurringReservationId) {
        recurringReservationService.cancel(recurringReservationId);
    }

    @PostMapping("/materialize")
    MaterializationSummaryResponse materialize() {
        RecurringReservationMaterializationSummary summary = materializationService.materializeDue();
        return new MaterializationSummaryResponse(summary.created(), summary.skipped());
    }

    private CreateRecurringReservationCommand toCommand(CreateRecurringReservationRequest request) {
        return new CreateRecurringReservationCommand(
                request.resourceId(),
                request.customerId(),
                DayOfWeek.valueOf(request.weekday()),
                request.startTime(),
                request.endTime(),
                request.windowMonths());
    }

    private RecurringReservationResponse toResponse(RecurringReservationInfo recurringReservation) {
        return new RecurringReservationResponse(
                recurringReservation.id(),
                recurringReservation.resourceId(),
                recurringReservation.customerId(),
                recurringReservation.weekday().name(),
                recurringReservation.startTime(),
                recurringReservation.endTime(),
                recurringReservation.windowMonths(),
                recurringReservation.status().name(),
                recurringReservation.nextOccurrence(),
                recurringReservation.createdAt(),
                recurringReservation.cancelledAt(),
                recurringReservation.cancelledBy());
    }

}
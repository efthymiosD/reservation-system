package com.decoupledx.reservation.administration.adapter.in.web;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.administration.adapter.api.AdministrationApi;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationMaterializationSummary;
import com.decoupledx.reservation.administration.adapter.api.CreateRecurringReservationCommand;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationStatus;
import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/recurring-reservations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class RecurringReservationAdminController {

    private final AdministrationApi administrationApi;
    private final CurrentCustomerApi currentCustomer;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RecurringReservationResponse create(@Valid @RequestBody CreateRecurringReservationRequest request) {
        return toResponse(administrationApi.createRecurringReservation(toCommand(request)));
    }

    @GetMapping
    List<RecurringReservationResponse> findRecurringReservations(
            @RequestParam(required = false) RecurringReservationStatus status) {
        return administrationApi.findRecurringReservations().stream()
                .filter(recurringReservation -> status == null || recurringReservation.status() == status)
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{recurringReservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID recurringReservationId) {
        administrationApi.cancelRecurringReservation(recurringReservationId, currentCustomer.currentCustomerId());
    }

    @PostMapping("/materialize")
    MaterializationSummaryResponse materialize() {
        RecurringReservationMaterializationSummary summary = administrationApi.materializeDue();
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

    record CreateRecurringReservationRequest(
            @NotNull UUID resourceId,
            @NotNull UUID customerId,
            @NotNull String weekday,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotNull Integer windowMonths) {
    }

    record RecurringReservationResponse(
            UUID id,
            UUID resourceId,
            UUID customerId,
            String weekday,
            LocalTime startTime,
            LocalTime endTime,
            int windowMonths,
            String status,
            LocalDate nextOccurrence,
            Instant createdAt,
            Instant cancelledAt,
            UUID cancelledBy) {
    }

    record MaterializationSummaryResponse(int created, int skipped) {
    }
}
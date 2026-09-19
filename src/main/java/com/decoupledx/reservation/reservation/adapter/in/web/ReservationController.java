package com.decoupledx.reservation.reservation.adapter.in.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.service.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.service.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
class ReservationController {

    private final CreateReservationService createReservation;
    private final CancelReservationService cancelReservation;
    private final ReservationQueryService reservationQueries;
    private final CurrentCustomerApi currentCustomer;
    private final ReservationViews views;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReservationResponse create(@Valid @RequestBody CreateReservationRequest request) {
        return views.from(createReservation.create(
                request.resourceId(),
                request.startTime(),
                request.durationMinutes(),
                currentCustomer.currentCustomerId()));
    }

    @GetMapping
    MyReservationsPage myReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return views.pageFrom(reservationQueries.findMyReservationsPage(
                currentCustomer.currentCustomerId(), status, page, size));
    }

    @GetMapping("/{reservationId}")
    ReservationResponse get(@PathVariable UUID reservationId) {
        return views.from(reservationQueries.getReservation(
                ReservationId.of(reservationId), currentCustomer.currentCustomerId()));
    }

    @PostMapping("/{reservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID reservationId) {
        cancelReservation.cancel(ReservationId.of(reservationId), currentCustomer.currentCustomerId());
    }

    record CreateReservationRequest(
            @NotNull UUID resourceId,
            @NotNull java.time.LocalDateTime startTime,
            @Positive int durationMinutes) {
    }
}

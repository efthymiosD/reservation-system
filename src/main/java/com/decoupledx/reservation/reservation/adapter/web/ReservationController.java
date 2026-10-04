package com.decoupledx.reservation.reservation.adapter.web;

import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.port.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
class ReservationController {

    private final CreateReservationService createReservation;
    private final CancelReservationService cancelReservation;
    private final ReservationQueryService reservationQueries;
    private final ReservationViews views;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReservationResponse create(@Valid @RequestBody CreateReservationRequest request) {
        return views.from(createReservation.create(
                request.resourceId(),
                request.startTime(),
                request.durationMinutes()));
    }

    @GetMapping
    MyReservationsPage myReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return views.pageFrom(reservationQueries.findMyReservationsPage(status, page, size));
    }

    @GetMapping("/{reservationId}")
    ReservationResponse get(@PathVariable UUID reservationId) {
        return views.from(reservationQueries.getReservation(reservationId));
    }

    @PostMapping("/{reservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID reservationId) {
        cancelReservation.cancel(reservationId);
    }

}

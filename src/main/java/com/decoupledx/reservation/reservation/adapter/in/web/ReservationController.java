package com.decoupledx.reservation.reservation.adapter.in.web;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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

import com.decoupledx.reservation.identity.adapter.in.CurrentCustomerResolver;
import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.service.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.model.CreateReservationCommand;
import com.decoupledx.reservation.reservation.domain.service.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.model.ReservationId;
import com.decoupledx.reservation.reservation.domain.model.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.reservation.domain.model.ReservationStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.venue.domain.service.VenueService;

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
    private final CurrentCustomerResolver currentCustomer;
    private final VenueService venueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReservationResponse create(@Valid @RequestBody CreateReservationRequest request) {
        CustomerId customer = currentCustomer.currentCustomerId();
        ZoneId zone = venueService.getVenue(venueService.singleVenueId()).timezone();
        Instant start = request.startTime().atZone(zone).toInstant();
        Instant end = start.plus(Duration.ofMinutes(request.durationMinutes()));
        CreateReservationCommand command = new CreateReservationCommand(
                ResourceId.of(request.resourceId()), start, end);
        return toResponse(createReservation.create(command, customer));
    }

    @GetMapping
    MyReservationsPage myReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ReservationQueryService.ReservationPage result =
                reservationQueries.findMyReservationsPage(currentCustomer.currentCustomerId(), status, page, size);
        List<ReservationResponse> items = result.items().stream().map(this::toResponse).toList();
        return new MyReservationsPage(items, result.total(), result.page(), result.size());
    }

    @GetMapping("/{reservationId}")
    ReservationResponse get(@PathVariable UUID reservationId) {
        ReservationInfo reservation = reservationQueries.getReservation(
                ReservationId.of(reservationId), currentCustomer.currentCustomerId());
        return toResponse(reservation);
    }

    @PostMapping("/{reservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID reservationId) {
        cancelReservation.cancel(ReservationId.of(reservationId), currentCustomer.currentCustomerId());
    }

    private ReservationResponse toResponse(ReservationInfo reservation) {
        return new ReservationResponse(
                reservation.id().value(),
                reservation.resourceId().value(),
                reservation.start(),
                reservation.end(),
                reservation.status().name(),
                reservation.price().amount(),
                reservation.price().currency().getCurrencyCode(),
                reservation.createdAt(),
                reservation.cancelledAt());
    }

    record CreateReservationRequest(
            @NotNull UUID resourceId,
            @NotNull LocalDateTime startTime,
            @Positive int durationMinutes) {
    }

    record ReservationResponse(
            UUID id,
            UUID resourceId,
            Instant start,
            Instant end,
            String status,
            BigDecimal priceAmount,
            String priceCurrency,
            Instant createdAt,
            Instant cancelledAt) {
    }

    record MyReservationsPage(
            List<ReservationResponse> items,
            long total,
            int page,
            int size) {
    }
}

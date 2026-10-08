package com.decoupledx.reservation.reservation.adapter.web;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class ReservationAdminController {

    private final ReservationQueryService reservationQueries;
    private final CreateReservationService createReservation;
    private final ReservationViews views;

    @GetMapping
    AdminReservationPage list(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ReservationPage result = reservationQueries.listAllForAdmin(status, page, size);
        List<AdminReservationResponse> items = result.items().stream()
                .map(r -> new AdminReservationResponse(
                        r.id().value(),
                        r.resourceId().value(),
                        r.customerId().value(),
                        r.start(),
                        r.end(),
                        r.status().name(),
                        r.price().amount(),
                        r.price().currency().getCurrencyCode(),
                        r.createdAt(),
                        r.cancelledAt(),
                        r.recurringReservationId()))
                .toList();
        return new AdminReservationPage(items, result.total(), result.page(), result.size());
    }

    /**
     * Books on behalf of a customer (staff placement). The same-customer
     * overlap invariant is relaxed — several bookings for one hour are
     * allowed; a single pitch still cannot host two bookings in the same
     * window (409).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReservationResponse createOnBehalfOfCustomer(@Valid @RequestBody CreateOnBehalfOfCustomerRequest request) {
        return views.from(createReservation.createOnBehalfOfCustomer(
                request.resourceId(),
                request.startTime(),
                request.durationMinutes(),
                CustomerId.of(request.customerId())));
    }
}

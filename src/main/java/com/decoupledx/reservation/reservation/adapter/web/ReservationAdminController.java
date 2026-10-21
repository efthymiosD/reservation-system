package com.decoupledx.reservation.reservation.adapter.web;

import com.decoupledx.reservation.identity.adapter.api.CustomerDirectoryApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.AdminReservationFilter;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.port.CreateReservationService;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final CustomerDirectoryApi customerDirectory;
    private final CreateReservationService createReservation;
    private final ReservationViews views;

    /**
     * Lists reservations under combined, optional constraints: status, one or
     * more customer NAMES (existing customers, case-insensitive equality — an
     * unknown name yields an explicitly empty page, never an unfiltered list)
     * and a start window of ISO-8601 UTC instants (start-in-range, inclusive
     * from, exclusive {@code startTimeTo}).
     */
    @GetMapping
    AdminReservationPage list(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) List<String> customer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant startTimeFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant startTimeTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CustomerId> matched = customer == null
                ? List.of()
                : customer.stream()
                        .flatMap(name -> customerDirectory.idsByExactName(name.trim()).stream())
                        .toList();
        if (customer != null && !customer.isEmpty() && matched.isEmpty()) {
            return explicitlyEmptyPage(page, size);
        }
        AdminReservationFilter filter = new AdminReservationFilter(status, matched, startTimeFrom, startTimeTo);
        ReservationPage result = reservationQueries.listAllForAdmin(filter, page, size);
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

    private AdminReservationPage explicitlyEmptyPage(int page, int size) {
        return new AdminReservationPage(List.of(), 0, Math.max(page, 0), size);
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

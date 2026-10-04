package com.decoupledx.reservation.reservation.adapter.web;

import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.port.ReservationQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class ReservationAdminController {

    private final ReservationQueryService reservationQueries;

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

}

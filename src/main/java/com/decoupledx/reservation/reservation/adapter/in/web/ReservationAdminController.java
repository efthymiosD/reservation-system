package com.decoupledx.reservation.reservation.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService.ReservationPage;
import com.decoupledx.reservation.reservation.domain.model.ReservationStatus;

import lombok.RequiredArgsConstructor;

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
        ReservationPage result = reservationQueries.findAllForAdmin(status, page, size);
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
                        r.cancelledAt()))
                .toList();
        return new AdminReservationPage(items, result.total(), result.page(), result.size());
    }

    record AdminReservationResponse(
            UUID id,
            UUID resourceId,
            String customerId,
            Instant start,
            Instant end,
            String status,
            BigDecimal priceAmount,
            String priceCurrency,
            Instant createdAt,
            Instant cancelledAt) {
    }

    record AdminReservationPage(
            List<AdminReservationResponse> items,
            long total,
            int page,
            int size) {
    }
}

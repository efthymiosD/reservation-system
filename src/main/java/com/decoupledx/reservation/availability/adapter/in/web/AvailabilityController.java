package com.decoupledx.reservation.availability.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.availability.domain.service.AvailabilityService;
import com.decoupledx.reservation.availability.domain.model.AvailableResource;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    List<AvailableResourceResponse> findAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam int durationMinutes) {
        return availabilityService.findAvailable(date, start, durationMinutes).stream()
                .map(this::toResponse)
                .toList();
    }

    private AvailableResourceResponse toResponse(AvailableResource resource) {
        return new AvailableResourceResponse(
                resource.resourceId(),
                resource.name(),
                resource.code(),
                resource.type(),
                resource.priceAmount(),
                resource.priceCurrency());
    }

    record AvailableResourceResponse(
            UUID resourceId,
            String name,
            String code,
            String type,
            BigDecimal priceAmount,
            String priceCurrency) {
    }
}

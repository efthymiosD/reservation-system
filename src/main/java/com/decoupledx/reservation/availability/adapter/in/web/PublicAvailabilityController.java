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

import com.decoupledx.reservation.availability.domain.model.ResourceAvailability;
import com.decoupledx.reservation.availability.domain.service.AvailabilityService;

import lombok.RequiredArgsConstructor;

/**
 * Public, read-only availability map for the venue: every active resource with its
 * status for one requested slot. Lets clients render the venue map before login;
 * reserving still requires authentication and full server-side revalidation.
 */
@RestController
@RequestMapping("/api/public/availability")
@RequiredArgsConstructor
class PublicAvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/map")
    AvailabilityMapResponse map(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam int durationMinutes) {
        List<ResourceAvailability> resources =
                availabilityService.findResourceAvailability(date, start, durationMinutes);
        return new AvailabilityMapResponse(
                date,
                start,
                durationMinutes,
                resources.stream().map(this::toResponse).toList());
    }

    private ResourceStatusResponse toResponse(ResourceAvailability resource) {
        return new ResourceStatusResponse(
                resource.resourceId(),
                resource.name(),
                resource.code(),
                resource.type(),
                resource.status().name(),
                resource.priceAmount(),
                resource.priceCurrency());
    }

    record AvailabilityMapResponse(
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            List<ResourceStatusResponse> resources) {
    }

    record ResourceStatusResponse(
            UUID resourceId,
            String name,
            String code,
            String type,
            String status,
            BigDecimal priceAmount,
            String priceCurrency) {
    }
}

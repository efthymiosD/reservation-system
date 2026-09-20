package com.decoupledx.reservation.availability.adapter.web;

import com.decoupledx.reservation.availability.adapter.api.ResourceAvailability;
import com.decoupledx.reservation.availability.domain.port.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
                availabilityService.resourceAvailability(date, start, durationMinutes);
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

}

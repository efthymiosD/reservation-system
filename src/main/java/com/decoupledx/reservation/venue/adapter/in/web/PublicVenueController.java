package com.decoupledx.reservation.venue.adapter.in.web;

import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.venue.domain.model.VenueInfo;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
class PublicVenueController {

    private final VenueService venueService;

    @GetMapping("/venue")
    PublicVenueResponse getVenue() {
        return toResponse(venueService.getPublicVenueInfo());
    }

    private PublicVenueResponse toResponse(VenueInfo venue) {
        Map<String, OpeningHoursResponse> openingHours = venue.openingHours().perDay().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        entry -> new OpeningHoursResponse(
                                entry.getValue().opensAt(),
                                entry.getValue().closesAt())));
        return new PublicVenueResponse(
                venue.name(),
                venue.description(),
                venue.address(),
                venue.timezone().getId(),
                openingHours);
    }

    record PublicVenueResponse(
            String name,
            String description,
            String address,
            String timezone,
            Map<String, OpeningHoursResponse> openingHours) {
    }

    record OpeningHoursResponse(LocalTime opensAt, LocalTime closesAt) {
    }
}

package com.decoupledx.reservation.venue.adapter.web;

import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import com.decoupledx.reservation.venue.domain.port.VenueService;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
class PublicVenueController {

    private final VenueService venueService;

    @GetMapping("/venue")
    PublicVenueResponse getVenue() {
        return toResponse(venueService.getVenue(venueService.singleVenueId()));
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

}

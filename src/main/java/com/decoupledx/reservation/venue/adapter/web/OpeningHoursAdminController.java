package com.decoupledx.reservation.venue.adapter.web;

import com.decoupledx.reservation.venue.adapter.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.domain.port.VenueService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/opening-hours")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class OpeningHoursAdminController {

    private final VenueService venueService;

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateOpeningHours(@Valid @RequestBody OpeningHoursUpdateRequest request) {
        OpeningHours openingHours = new OpeningHours(request.days().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new DailyOpeningHours(
                                entry.getValue().opensAt(),
                                entry.getValue().closesAt()))));
        venueService.updateOpeningHours(venueService.singleVenueId(), openingHours);
    }

}

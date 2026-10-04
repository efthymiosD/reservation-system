package com.decoupledx.reservation.venue.adapter.web;

import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import com.decoupledx.reservation.venue.domain.port.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/venue")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class VenueAdminController {

    private final VenueService venueService;

    @GetMapping
    VenueInfo get() {
        return venueService.getVenue(venueService.singleVenueId());
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@Valid @RequestBody VenueProfileUpdateRequest request) {
        venueService.updateProfile(
                venueService.singleVenueId(),
                request.name(),
                request.description(),
                request.address());
    }

}

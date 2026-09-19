package com.decoupledx.reservation.venue.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/venue")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class VenueAdminController {

    private final VenueService venueService;

    @GetMapping
    VenueInfo get() {
        return venueService.getPublicVenueInfo();
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

    record VenueProfileUpdateRequest(
            @NotBlank String name,
            String description,
            String address) {
    }
}
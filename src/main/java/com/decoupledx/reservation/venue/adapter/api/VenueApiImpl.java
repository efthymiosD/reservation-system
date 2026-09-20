package com.decoupledx.reservation.venue.adapter.api;

import java.util.UUID;

import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.domain.port.VenueService;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link VenueApi} on top of the
 * venue inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class VenueApiImpl implements VenueApi {

    private final VenueService venueService;

    @Override
    public VenueInfo getVenue(UUID venueId) {
        return venueService.getVenue(venueId);
    }

    @Override
    public void updateOpeningHours(UUID venueId, OpeningHours openingHours) {
        venueService.updateOpeningHours(venueId, openingHours);
    }

    @Override
    public void updateProfile(UUID venueId, String name, String description, String address) {
        venueService.updateProfile(venueId, name, description, address);
    }

    @Override
    public UUID singleVenueId() {
        return venueService.singleVenueId();
    }
}

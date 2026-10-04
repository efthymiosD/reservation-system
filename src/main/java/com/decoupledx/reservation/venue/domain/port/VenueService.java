package com.decoupledx.reservation.venue.domain.port;

import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import java.util.UUID;

/**
 * Inbound port for the venue module: venue profile and opening-hours access.
 */
public interface VenueService {

    VenueInfo getVenue(UUID venueId);

    void updateOpeningHours(UUID venueId, OpeningHours openingHours);

    void updateProfile(UUID venueId, String name, String description, String address);

    UUID singleVenueId();
}

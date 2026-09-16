package com.decoupledx.reservation.venue.api;

import java.util.UUID;

/**
 * Module API of the venue module. This is the only type other modules may
 * depend on; the implementation stays module-internal.
 */
public interface VenueApi {

    UUID singleVenueId();

    VenueInfo getVenue(UUID venueId);

    void updateOpeningHours(UUID venueId, OpeningHours openingHours);

    void updateProfile(UUID venueId, String name, String description, String address);
}

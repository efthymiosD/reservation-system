package com.decoupledx.reservation.venue.adapter.persistence;

import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;
import java.util.UUID;

/**
 * Persistence view of a venue: the form {@link VenueRepository} uses internally.
 */
public record VenueDataValue(UUID id, String name, String description, String address,
                             String timezone, OpeningHours openingHours) {
}

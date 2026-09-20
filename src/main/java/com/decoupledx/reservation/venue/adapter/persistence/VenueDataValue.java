package com.decoupledx.reservation.venue.adapter.persistence;

import java.util.UUID;

import com.decoupledx.reservation.venue.adapter.api.OpeningHours;

/**
 * Persistence view of a venue: the form {@link VenueRepository} uses internally.
 */
public record VenueDataValue(UUID id, String name, String description, String address,
                      String timezone, OpeningHours openingHours) {
}

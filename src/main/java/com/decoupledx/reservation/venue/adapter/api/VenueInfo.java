package com.decoupledx.reservation.venue.adapter.api;

import java.time.ZoneId;

public record VenueInfo(
        VenueId id,
        String name,
        String description,
        String address,
        ZoneId timezone,
        OpeningHours openingHours) {
}

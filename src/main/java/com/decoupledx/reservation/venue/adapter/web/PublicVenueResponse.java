package com.decoupledx.reservation.venue.adapter.web;

import java.util.Map;

record PublicVenueResponse(
        String name,
        String description,
        String address,
        String timezone,
        Map<String, OpeningHoursResponse> openingHours) {
}

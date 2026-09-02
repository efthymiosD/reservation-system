package com.decoupledx.reservation.venue.domain.model;

import java.time.ZoneId;
import java.util.Objects;

import com.decoupledx.reservation.venue.domain.model.OpeningHours;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.Getter;

@Getter
public class Venue {

    private final VenueId id;
    private String name;
    private String description;
    private String address;
    private final ZoneId timezone;
    private OpeningHours openingHours;

    public Venue(VenueId id, String name, String description, String address,
                 ZoneId timezone, OpeningHours openingHours) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.address = address;
        this.timezone = Objects.requireNonNull(timezone, "timezone must not be null");
        this.openingHours = Objects.requireNonNull(openingHours, "openingHours must not be null");
    }

    public void updateOpeningHours(OpeningHours openingHours) {
        this.openingHours = Objects.requireNonNull(openingHours, "openingHours must not be null");
    }
}

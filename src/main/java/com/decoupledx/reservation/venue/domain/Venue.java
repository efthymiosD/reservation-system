package com.decoupledx.reservation.venue.domain;

import java.time.ZoneId;
import java.util.Objects;

import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import com.decoupledx.reservation.venue.adapter.persistence.VenueDataValue;

import lombok.Getter;

@Getter
class Venue {

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

    public void updateProfile(String name, String description, String address) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.address = address;
    }

    VenueDataValue toDataValue() {
        return new VenueDataValue(id.value(), name, description, address,
                timezone.getId(), openingHours);
    }

    VenueInfo toInfo() {
        return new VenueInfo(id, name, description, address, timezone, openingHours);
    }
}

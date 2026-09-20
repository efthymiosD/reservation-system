package com.decoupledx.reservation.venue.domain.port;

import java.util.List;
import java.util.Optional;

import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.venue.adapter.persistence.VenueDataValue;

public interface VenueRepository {

    Optional<VenueDataValue> findById(VenueId id);

    List<VenueDataValue> findAll();

    VenueDataValue save(VenueDataValue venue);
}

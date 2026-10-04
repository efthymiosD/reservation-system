package com.decoupledx.reservation.venue.domain.port;

import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.venue.adapter.persistence.VenueDataValue;
import java.util.List;
import java.util.Optional;

public interface VenueRepository {

    Optional<VenueDataValue> findById(VenueId id);

    List<VenueDataValue> findAll();

    VenueDataValue save(VenueDataValue venue);
}

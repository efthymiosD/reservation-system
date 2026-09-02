package com.decoupledx.reservation.venue.domain.port;

import java.util.List;
import java.util.Optional;

import com.decoupledx.reservation.venue.domain.model.VenueId;
import com.decoupledx.reservation.venue.domain.model.Venue;

public interface VenueRepository {

    Optional<Venue> findById(VenueId id);

    List<Venue> findAll();

    Venue save(Venue venue);
}

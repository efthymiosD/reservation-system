package com.decoupledx.reservation.policy.domain.port;

import java.util.Optional;

import com.decoupledx.reservation.policy.api.BookingPolicy;
import com.decoupledx.reservation.venue.api.VenueId;

public interface BookingPolicyRepository {

    Optional<BookingPolicy> findByVenueId(VenueId venueId);

    void save(VenueId venueId, BookingPolicy policy);
}

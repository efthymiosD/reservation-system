package com.decoupledx.reservation.policy.domain.port;

import com.decoupledx.reservation.policy.adapter.persistence.BookingPolicyDataValue;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.util.Optional;

public interface BookingPolicyRepository {

    Optional<BookingPolicyDataValue> findByVenueId(VenueId venueId);

    void save(VenueId venueId, BookingPolicyDataValue policy);
}

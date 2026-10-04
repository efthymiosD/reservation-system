package com.decoupledx.reservation.availability.domain;

import com.decoupledx.reservation.shared.Money;
import com.decoupledx.reservation.shared.ReservationPeriod;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;

record Slot(VenueInfo venue, ReservationPeriod period, Money price) {
}

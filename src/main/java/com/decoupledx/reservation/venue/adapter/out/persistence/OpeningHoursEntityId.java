package com.decoupledx.reservation.venue.adapter.out.persistence;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.UUID;

record OpeningHoursEntityId(UUID venueId, DayOfWeek dayOfWeek) implements Serializable {
}

package com.decoupledx.reservation.venue.adapter.web;

import java.time.LocalTime;

record OpeningHoursResponse(LocalTime opensAt, LocalTime closesAt) {
}

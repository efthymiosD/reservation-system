package com.decoupledx.reservation.venue.adapter.web;

import java.time.LocalTime;

public record OpeningHoursResponse(LocalTime opensAt, LocalTime closesAt) {
}

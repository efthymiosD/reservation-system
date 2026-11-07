package com.decoupledx.reservation.venue.adapter.web;

import jakarta.validation.constraints.NotBlank;

record VenueProfileUpdateRequest(
        @NotBlank String name,
        String description,
        String address) {
}

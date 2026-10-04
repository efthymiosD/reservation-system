package com.decoupledx.reservation.venue.adapter.web;

import jakarta.validation.constraints.NotBlank;

public record VenueProfileUpdateRequest(
        @NotBlank String name,
        String description,
        String address) {
}

package com.decoupledx.reservation.resource.adapter.web;

import jakarta.validation.constraints.NotBlank;

public record RenameRequest(@NotBlank String name) {
}

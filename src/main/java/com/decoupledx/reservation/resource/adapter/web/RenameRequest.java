package com.decoupledx.reservation.resource.adapter.web;

import jakarta.validation.constraints.NotBlank;

record RenameRequest(@NotBlank String name) {
}

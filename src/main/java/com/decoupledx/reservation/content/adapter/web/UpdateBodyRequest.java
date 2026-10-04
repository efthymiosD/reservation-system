package com.decoupledx.reservation.content.adapter.web;

import jakarta.validation.constraints.NotNull;

public record UpdateBodyRequest(@NotNull String body) {
}

package com.decoupledx.reservation.content.adapter.web;

import jakarta.validation.constraints.NotNull;

record UpdateBodyRequest(@NotNull String body) {
}

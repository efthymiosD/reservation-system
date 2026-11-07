package com.decoupledx.reservation.policy.adapter.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

record CancellationPolicyUpdateRequest(
        @NotNull @PositiveOrZero Integer deadlineBeforeStartMinutes) {
}

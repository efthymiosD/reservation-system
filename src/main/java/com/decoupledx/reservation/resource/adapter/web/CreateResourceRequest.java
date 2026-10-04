package com.decoupledx.reservation.resource.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateResourceRequest(
        @NotNull UUID groupId,
        @NotBlank String name,
        @NotBlank String code,
        @NotBlank String type) {
}

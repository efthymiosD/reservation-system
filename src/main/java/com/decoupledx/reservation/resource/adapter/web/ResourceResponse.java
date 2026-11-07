package com.decoupledx.reservation.resource.adapter.web;

import java.util.UUID;

record ResourceResponse(
        UUID id,
        UUID groupId,
        String name,
        String code,
        String type,
        String status) {
}

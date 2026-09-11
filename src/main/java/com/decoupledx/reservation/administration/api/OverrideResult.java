package com.decoupledx.reservation.administration.api;

import com.decoupledx.reservation.resource.api.ResourceBlockInfo;

/**
 * Outcome of the admin override use case: the created block plus the number of
 * reservations that were cancelled to make room for it.
 */
public record OverrideResult(ResourceBlockInfo block, int cancelledReservations) {
}

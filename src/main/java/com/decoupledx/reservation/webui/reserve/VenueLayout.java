package com.decoupledx.reservation.webui.reserve;

import java.util.List;

/**
 * Visual venue layout — presentation configuration, deliberately outside the
 * reservation domain. Placements are keyed by resource id; resources without a
 * placement are simply not drawn. A future admin layout editor writes this data;
 * the renderer consumes it and never hardcodes the arrangement.
 */
public record VenueLayout(int canvasWidth, int canvasHeight, List<Placement> placements) {

}

package com.decoupledx.reservation.webui.admin;

import java.util.List;
import java.util.UUID;

/**
 * View model for the /admin/map field-layout editor page.
 */
record AdminMapModel(
        int canvasWidth,
        int canvasHeight,
        List<MapFieldView> fields,
        UUID venueId) {

}

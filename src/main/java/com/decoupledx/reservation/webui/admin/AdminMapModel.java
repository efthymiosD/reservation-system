package com.decoupledx.reservation.webui.admin;

import java.util.List;
import java.util.UUID;

/** View model for the /admin/map field-layout editor page. */
public record AdminMapModel(
        int canvasWidth,
        int canvasHeight,
        List<MapFieldView> fields,
        UUID venueId) {

    public record MapFieldView(UUID resourceId, String name, String code, boolean active,
            int x, int y, int width, int height) {
    }
}
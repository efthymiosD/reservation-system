package com.decoupledx.reservation.webui.reserve;

import java.util.UUID;

record MapField(
        UUID resourceId,
        String label,
        int x,
        int y,
        int width,
        int height,
        String status,
        String statusClass,
        String ariaLabel) {
}

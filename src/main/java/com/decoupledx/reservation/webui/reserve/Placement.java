package com.decoupledx.reservation.webui.reserve;

import java.util.UUID;

public record Placement(UUID resourceId, int x, int y, int width, int height) {
}

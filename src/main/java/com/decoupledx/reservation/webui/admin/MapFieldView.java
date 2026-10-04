package com.decoupledx.reservation.webui.admin;

import java.util.UUID;

record MapFieldView(UUID resourceId, String name, String code, boolean active,
                    int x, int y, int width, int height) {
}

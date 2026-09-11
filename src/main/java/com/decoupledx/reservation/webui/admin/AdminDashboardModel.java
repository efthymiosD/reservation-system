package com.decoupledx.reservation.webui.admin;

import java.util.List;

/**
 * View model of the admin dashboard: quick counts plus navigation shortcuts.
 */
public record AdminDashboardModel(
        long activeReservations,
        long activeBlocks,
        long resources) {
}

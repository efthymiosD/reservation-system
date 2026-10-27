package com.decoupledx.reservation.webui.admin;

/**
 * View model of the admin dashboard: quick counts plus navigation shortcuts.
 */
record AdminDashboardModel(
        long activeReservations,
        long pendingBookingRequests,
        long activeRecurringReservations,
        long resources) {
}
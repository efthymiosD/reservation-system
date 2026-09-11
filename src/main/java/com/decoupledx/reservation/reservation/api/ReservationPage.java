package com.decoupledx.reservation.reservation.api;

import java.util.List;

/**
 * Page view over reservation listings crossing the module API
 * (customer listings and the admin listing).
 */
public record ReservationPage(List<ReservationInfo> items, long total, int page, int size) {
}

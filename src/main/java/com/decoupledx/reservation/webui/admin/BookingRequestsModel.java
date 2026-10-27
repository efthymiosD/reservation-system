package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkInfo;

import java.util.List;

/**
 * View model of the coach's booking-requests queue: resolved rows, the total
 * and the current shareable link (actual state only — the RAW token travels
 * via the flash of a regeneration action, never through this model).
 */
record BookingRequestsModel(
        List<BookingRequestRow> rows,
        long total,
        BookingRequestLinkInfo activeLink) {
}

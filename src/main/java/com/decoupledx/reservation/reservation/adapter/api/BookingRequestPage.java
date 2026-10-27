package com.decoupledx.reservation.reservation.adapter.api;

import java.util.List;

public record BookingRequestPage(
        List<BookingRequestInfo> items,
        long total,
        int page,
        int size) {
}

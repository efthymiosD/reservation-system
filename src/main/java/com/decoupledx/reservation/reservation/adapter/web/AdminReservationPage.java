package com.decoupledx.reservation.reservation.adapter.web;

import java.util.List;

public record AdminReservationPage(
        List<AdminReservationResponse> items,
        long total,
        int page,
        int size) {
}

package com.decoupledx.reservation.reservation.adapter.web;

import java.util.List;

record AdminReservationPage(
        List<AdminReservationResponse> items,
        long total,
        int page,
        int size) {
}

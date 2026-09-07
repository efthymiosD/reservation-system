package com.decoupledx.reservation.reservation.adapter.in.web;

import java.util.List;

record MyReservationsPage(
        List<ReservationResponse> items,
        long total,
        int page,
        int size) {
}

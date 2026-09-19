package com.decoupledx.reservation.webui.admin;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.administration.adapter.api.AdministrationApi;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;

import lombok.RequiredArgsConstructor;

/**
 * Assembles the admin dashboard counts from module APIs only.
 */
@Component
@RequiredArgsConstructor
class AdminDashboardFactory {

    private final ReservationApi reservationApi;
    private final AdministrationApi administrationApi;
    private final ResourceApi resourceService;
    private final VenueApi venueService;

    AdminDashboardModel build() {
        return new AdminDashboardModel(
                activeReservations(),
                administrationApi.findRecurringReservations().stream().filter(RecurringReservationInfo::isActive).count(),
                resourceService.findResources(venueService.singleVenueId()).size());
    }

    private long activeReservations() {
        ReservationPage page = reservationApi.listAllForAdmin(ReservationStatus.ACTIVE, 0, 1);
        return page.total();
    }
}
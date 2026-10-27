package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.administration.adapter.api.AdministrationApi;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.*;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Assembles the admin dashboard counts from module APIs only.
 */
@Component
@RequiredArgsConstructor
class AdminDashboardFactory {

    private final ReservationApi reservationApi;
    private final BookingRequestApi bookingRequestApi;
    private final AdministrationApi administrationApi;
    private final ResourceApi resourceService;
    private final VenueApi venueService;

    AdminDashboardModel build() {
        return new AdminDashboardModel(
                activeReservations(),
                pendingBookingRequests(),
                administrationApi.findRecurringReservations().stream().filter(RecurringReservationInfo::isActive).count(),
                resourceService.findResources(venueService.singleVenueId()).size());
    }

    private long activeReservations() {
        return reservationApi
                .listAllForAdmin(AdminReservationFilter.byStatus(ReservationStatus.ACTIVE), 0, 1)
                .total();
    }

    private long pendingBookingRequests() {
        return bookingRequestApi.listForAdmin(BookingRequestStatus.PENDING, 0, 1).total();
    }
}
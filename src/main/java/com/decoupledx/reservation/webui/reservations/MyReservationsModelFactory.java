package com.decoupledx.reservation.webui.reservations;

import java.time.Clock;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;

import lombok.RequiredArgsConstructor;

/**
 * Assembles the 'My reservations' view model: upcoming reservations (active and
 * in the future) first, everything else in the past section, field names from
 * the resource module, and the canCancel hint from the current cancellation
 * policy. If listings outgrow the fetched pages, further pages are appended.
 */
@Component
@RequiredArgsConstructor
class MyReservationsModelFactory {

    private static final int PAGE_SIZE = 100;

    private final ReservationApi reservationApi;
    private final ResourceApi resourceService;
    private final PolicyApi policyService;
    private final VenueApi venueService;
    private final CurrentCustomerApi currentCustomer;
    private final Clock clock;

    MyReservationsModel build() {
        CustomerId customer = currentCustomer.currentCustomerId();
        UUID venueId = venueService.singleVenueId();
        var deadline = policyService.cancellationPolicyFor(venueId).deadlineBeforeStart();
        var zone = venueService.getVenue(venueId).timezone();

        List<ReservationInfo> reservations = ownReservations(customer);
        var now = clock.instant();

        List<MyReservationsModel.ReservationCard> upcoming = new ArrayList<>();
        List<MyReservationsModel.ReservationCard> past = new ArrayList<>();
        for (ReservationInfo reservation : reservations) {
            boolean future = reservation.isActive() && !reservation.start().isBefore(now);
            var card = toCard(reservation, zone, deadline, now, future);
            (future ? upcoming : past).add(card);
        }
        upcoming.sort(Comparator.comparing(MyReservationsModel.ReservationCard::start));
        past.sort(Comparator.comparing(MyReservationsModel.ReservationCard::start).reversed());
        return new MyReservationsModel(upcoming, past);
    }

    private List<ReservationInfo> ownReservations(CustomerId customer) {
        List<ReservationInfo> all = new ArrayList<>();
        int page = 0;
        ReservationPage result;
        do {
            result = reservationApi.findMyReservationsPage(customer, null, page, PAGE_SIZE);
            all.addAll(result.items());
            page++;
        } while (all.size() < result.total() && page < result.page() + 2 && page < 10);
        return all;
    }

    private MyReservationsModel.ReservationCard toCard(ReservationInfo reservation,
            java.time.ZoneId zone, java.time.Duration deadline, java.time.Instant now, boolean future) {
        String fieldName = fieldName(reservation.resourceId());
        LocalDate date = reservation.start().atZone(zone).toLocalDate();
        LocalTime start = reservation.start().atZone(zone).toLocalTime();
        LocalTime end = reservation.end().atZone(zone).toLocalTime();
        String displayStatus = !reservation.isActive() ? "Cancelled"
                : future ? "Active" : "Completed";
        return new MyReservationsModel.ReservationCard(
                reservation.id().value(),
                fieldName,
                date,
                start,
                end,
                reservation.price().amount(),
                reservation.price().currency().getCurrencyCode(),
                displayStatus,
                reservation.isCancellable(now, deadline));
    }

    private String fieldName(ResourceId resourceId) {
        try {
            return resourceService.getResource(resourceId.value()).name();
        } catch (BusinessException gone) {
            return "the selected field";
        }
    }
}

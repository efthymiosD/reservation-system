package com.decoupledx.reservation.webui.reservations;

import java.time.Clock;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.api.CurrentCustomerApi;
import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.policy.api.PolicyApi;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.reservation.api.ReservationInfo;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.venue.api.VenueApi;

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

        List<ReservationInfo> reservations = ownReservations(customer, 100);
        var now = clock.instant();

        List<MyReservationsModel.ReservationCard> upcoming = new ArrayList<>();
        List<MyReservationsModel.ReservationCard> past = new ArrayList<>();
        for (ReservationInfo reservation : reservations) {
            var card = toCard(reservation, zone, deadline, now);
            boolean future = reservation.isActive() && !reservation.start().isBefore(now);
            (future ? upcoming : past).add(card);
        }
        upcoming.sort(Comparator.comparing(MyReservationsModel.ReservationCard::start));
        past.sort(Comparator.comparing(MyReservationsModel.ReservationCard::start).reversed());
        return new MyReservationsModel(upcoming, past);
    }

    private List<ReservationInfo> ownReservations(CustomerId customer, int pageSize) {
        List<ReservationInfo> all = new ArrayList<>();
        int page = 0;
        ReservationApi.ReservationPage result;
        do {
            result = reservationApi.findMyReservationsPage(customer, null, page, pageSize);
            all.addAll(result.items());
            page++;
        } while (all.size() < result.total() && page < result.page() + 2 && page < 10);
        return all;
    }

    private MyReservationsModel.ReservationCard toCard(ReservationInfo reservation,
            java.time.ZoneId zone, java.time.Duration deadline, java.time.Instant now) {
        String fieldName = fieldName(reservation.resourceId());
        LocalDate date = reservation.start().atZone(zone).toLocalDate();
        LocalTime start = reservation.start().atZone(zone).toLocalTime();
        LocalTime end = reservation.end().atZone(zone).toLocalTime();
        return new MyReservationsModel.ReservationCard(
                reservation.id().value(),
                fieldName,
                date,
                start,
                end,
                reservation.price().amount(),
                reservation.price().currency().getCurrencyCode(),
                reservation.status().name(),
                reservation.isCancellable(now, deadline));
    }

    private String fieldName(com.decoupledx.reservation.resource.api.ResourceId resourceId) {
        try {
            return resourceService.getResource(resourceId.value()).name();
        } catch (com.decoupledx.reservation.shared.domain.BusinessException gone) {
            return "the selected field";
        }
    }
}

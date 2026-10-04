package com.decoupledx.reservation.webui.reservations;

import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    private final Clock clock;

    MyReservationsModel build() {
        UUID venueId = venueService.singleVenueId();
        var deadline = policyService.cancellationPolicyFor(venueId).deadlineBeforeStart();
        var zone = venueService.getVenue(venueId).timezone();

        List<ReservationInfo> reservations = ownReservations();
        var now = clock.instant();

        List<ReservationCard> upcoming = new ArrayList<>();
        List<ReservationCard> past = new ArrayList<>();
        for (ReservationInfo reservation : reservations) {
            boolean future = reservation.isActive() && !reservation.start().isBefore(now);
            var card = toCard(reservation, zone, deadline, now, future);
            (future ? upcoming : past).add(card);
        }
        upcoming.sort(Comparator.comparing(ReservationCard::start));
        past.sort(Comparator.comparing(ReservationCard::start).reversed());
        return new MyReservationsModel(upcoming, past);
    }

    private List<ReservationInfo> ownReservations() {
        List<ReservationInfo> all = new ArrayList<>();
        int page = 0;
        ReservationPage result;
        do {
            result = reservationApi.findMyReservationsPage(null, page, PAGE_SIZE);
            all.addAll(result.items());
            page++;
        } while (all.size() < result.total() && page < result.page() + 2 && page < 10);
        return all;
    }

    private ReservationCard toCard(ReservationInfo reservation,
                                   ZoneId zone, Duration deadline, Instant now, boolean future) {
        String fieldName = fieldName(reservation.resourceId());
        LocalDate date = reservation.start().atZone(zone).toLocalDate();
        LocalTime start = reservation.start().atZone(zone).toLocalTime();
        LocalTime end = reservation.end().atZone(zone).toLocalTime();
        String displayStatus = !reservation.isActive() ? "Cancelled"
                : future ? "Active" : "Completed";
        return new ReservationCard(
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

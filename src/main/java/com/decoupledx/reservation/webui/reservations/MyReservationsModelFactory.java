package com.decoupledx.reservation.webui.reservations;

import com.decoupledx.reservation.policy.adapter.api.PolicyApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationPage;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.webui.WebMessages;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final WebMessages messages;
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
        var fieldNames = fieldNames(reservations);

        List<ReservationCard> upcoming = new ArrayList<>();
        List<ReservationCard> past = new ArrayList<>();
        for (ReservationInfo reservation : reservations) {
            boolean future = reservation.isActive() && !reservation.start().isBefore(now);
            var card = toCard(reservation, fieldNames, zone, deadline, now, future);
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
        } while (all.size() < result.total() && page < 10);
        // page < 10: a hard cap of ~1000 reservations — more than any card
        // overview should render; deep pages live in the API, not the UI.
        return all;
    }

    private ReservationCard toCard(ReservationInfo reservation,
                                   java.util.Map<UUID, String> fieldNames,
                                   ZoneId zone, Duration deadline, Instant now, boolean future) {
        String fieldName = fieldName(reservation.resourceId(), fieldNames);
        LocalDate date = reservation.start().atZone(zone).toLocalDate();
        LocalTime start = reservation.start().atZone(zone).toLocalTime();
        LocalTime end = reservation.end().atZone(zone).toLocalTime();
        String displayStatus = !reservation.isActive() ? messages.get("myres.status.cancelled")
                : future ? messages.get("myres.status.active") : messages.get("myres.status.completed");
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

    private String fieldName(ResourceId resourceId, Map<UUID, String> fieldNames) {
        return fieldNames.getOrDefault(resourceId.value(), messages.get("reserve.theSelectedField"));
    }

    /** ONE batch name lookup for the whole listing (AGENTS.md — no N+1). */
    private Map<UUID, String> fieldNames(List<ReservationInfo> reservations) {
        Set<UUID> ids = reservations.stream()
                .map(reservation -> reservation.resourceId().value())
                .collect(Collectors.toSet());
        return resourceService.resourceNames(ids);
    }
}

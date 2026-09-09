package com.decoupledx.reservation.webui.confirmation;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.reservation.api.ReservationInfo;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.venue.api.VenueApi;

import lombok.RequiredArgsConstructor;

/**
 * Assembles the confirmation view model for one of the current customer's
 * reservations: field name from the resource module, venue-local schedule, and
 * the stored price snapshot.
 */
@Component
@RequiredArgsConstructor
class ConfirmationModelFactory {

    private final ReservationApi reservationQueries;
    private final ResourceApi resourceService;
    private final VenueApi venueService;

    ConfirmationModel build(UUID reservationId, CustomerId customer) {
        ReservationInfo reservation = reservationQueries.getReservation(reservationId, customer);
        String fieldName = resourceService.getResource(reservation.resourceId().value()).name();
        ZoneId zone = venueService.getVenue(venueService.singleVenueId()).timezone();

        LocalDate date = reservation.start().atZone(zone).toLocalDate();
        LocalTime start = reservation.start().atZone(zone).toLocalTime();
        LocalTime end = reservation.end().atZone(zone).toLocalTime();
        long durationMinutes = Duration.between(reservation.start(), reservation.end()).toMinutes();

        return new ConfirmationModel(
                reservation.id().value(),
                fieldName,
                date,
                start,
                end,
                durationMinutes,
                reservation.price().amount(),
                reservation.price().currency().getCurrencyCode());
    }
}

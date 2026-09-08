package com.decoupledx.reservation.webui;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.decoupledx.reservation.venue.domain.model.DailyOpeningHours;
import com.decoupledx.reservation.venue.domain.model.VenueInfo;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import lombok.RequiredArgsConstructor;

/**
 * Exposes the venue and its weekly opening hours to every web UI view, keeping
 * page controllers free of model assembly and templates free of lookups.
 */
@ControllerAdvice
@RequiredArgsConstructor
class VenueAdvice {

    private final VenueService venueService;

    @ModelAttribute("venue")
    VenueInfo venue() {
        return venueService.getVenue(venueService.singleVenueId());
    }

    /** Monday-first map of each weekday to its hours; empty Optional when closed. */
    @ModelAttribute("weeklyHours")
    Map<DayOfWeek, Optional<DailyOpeningHours>> weeklyHours() {
        VenueInfo venue = venue();
        Map<DayOfWeek, Optional<DailyOpeningHours>> week = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            week.put(day, venue.openingHours().on(day));
        }
        return week;
    }
}

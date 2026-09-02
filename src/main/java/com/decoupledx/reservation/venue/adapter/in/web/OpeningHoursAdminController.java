package com.decoupledx.reservation.venue.adapter.in.web;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.venue.domain.model.DailyOpeningHours;
import com.decoupledx.reservation.venue.domain.model.OpeningHours;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/opening-hours")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class OpeningHoursAdminController {

    private final VenueService venueService;

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateOpeningHours(@Valid @RequestBody OpeningHoursUpdateRequest request) {
        OpeningHours openingHours = new OpeningHours(request.days().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new DailyOpeningHours(
                                entry.getValue().opensAt(),
                                entry.getValue().closesAt()))));
        venueService.updateOpeningHours(venueService.singleVenueId(), openingHours);
    }

    record OpeningHoursUpdateRequest(
            @NotEmpty Map<DayOfWeek, @NotNull DailyOpeningHoursRequest> days) {
    }

    record DailyOpeningHoursRequest(
            @NotNull LocalTime opensAt,
            @NotNull LocalTime closesAt) {
    }
}

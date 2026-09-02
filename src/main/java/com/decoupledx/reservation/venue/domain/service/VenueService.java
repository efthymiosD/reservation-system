package com.decoupledx.reservation.venue.domain.service;

import java.util.List;
import com.decoupledx.reservation.venue.domain.model.OpeningHours;
import com.decoupledx.reservation.venue.domain.model.VenueId;
import com.decoupledx.reservation.venue.domain.model.VenueInfo;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.venue.domain.model.Venue;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final TransactionRunner tx;

    public VenueInfo getPublicVenueInfo() {
        return toInfo(singleVenue());
    }

    public VenueInfo getVenue(VenueId venueId) {
        return toInfo(findVenue(venueId));
    }

    public VenueId singleVenueId() {
        return singleVenue().getId();
    }

    public void updateOpeningHours(VenueId venueId, OpeningHours openingHours) {
        tx.run(() -> {
            Venue venue = findVenue(venueId);
            venue.updateOpeningHours(openingHours);
            venueRepository.save(venue);
        });
    }

    private Venue findVenue(VenueId venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VENUE_NOT_FOUND));
    }

    private Venue singleVenue() {
        List<Venue> venues = venueRepository.findAll();
        if (venues.size() != 1) {
            throw new IllegalStateException("Expected exactly one venue, found " + venues.size());
        }
        return venues.getFirst();
    }

    private VenueInfo toInfo(Venue venue) {
        return new VenueInfo(
                venue.getId(),
                venue.getName(),
                venue.getDescription(),
                venue.getAddress(),
                venue.getTimezone(),
                venue.getOpeningHours());
    }
}

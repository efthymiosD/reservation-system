package com.decoupledx.reservation.venue.domain.service;

import java.util.List;
import java.util.UUID;
import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;

import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.domain.model.Venue;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VenueService implements VenueApi {

    private final VenueRepository venueRepository;
    private final TransactionRunner tx;

    public VenueInfo getPublicVenueInfo() {
        return toInfo(singleVenue());
    }

    @Override
    public VenueInfo getVenue(UUID venueId) {
        return getVenue(VenueId.of(venueId));
    }

    public VenueInfo getVenue(VenueId venueId) {
        return toInfo(findVenue(venueId));
    }

    @Override
    public UUID singleVenueId() {
        return singleVenueIdInternal().value();
    }

    private VenueId singleVenueIdInternal() {
        return singleVenue().getId();
    }

    @Override
    public void updateOpeningHours(UUID venueId, OpeningHours openingHours) {
        tx.run(() -> {
            Venue venue = findVenue(venueId);
            venue.updateOpeningHours(openingHours);
            venueRepository.save(venue);
        });
    }

    @Override
    public void updateProfile(UUID venueId, String name, String description, String address) {
        tx.run(() -> {
            if (name == null || name.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_VENUE_NAME);
            }
            Venue venue = findVenue(venueId);
            venue.updateProfile(name, description, address);
            venueRepository.save(venue);
        });
    }

    private Venue findVenue(UUID venueId) {
        return findVenue(VenueId.of(venueId));
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

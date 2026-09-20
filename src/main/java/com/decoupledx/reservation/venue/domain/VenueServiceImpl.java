package com.decoupledx.reservation.venue.domain;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import com.decoupledx.reservation.venue.adapter.persistence.VenueDataValue;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;
import com.decoupledx.reservation.venue.domain.port.VenueService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final TransactionRunner tx;

    @Override
    public VenueInfo getVenue(UUID venueId) {
        return toDomain(venueRepository.findById(VenueId.of(venueId))
                .orElseThrow(() -> new BusinessException(ErrorCode.VENUE_NOT_FOUND)))
                .toInfo();
    }

    @Override
    public void updateOpeningHours(UUID venueId, OpeningHours openingHours) {
        tx.run(() -> {
            Venue venue = findVenue(VenueId.of(venueId));
            venue.updateOpeningHours(openingHours);
            venueRepository.save(venue.toDataValue());
        });
    }

    @Override
    public void updateProfile(UUID venueId, String name, String description, String address) {
        tx.run(() -> {
            Venue venue = findVenue(VenueId.of(venueId));
            venue.updateProfile(name, description, address);
            venueRepository.save(venue.toDataValue());
        });
    }

    @Override
    public UUID singleVenueId() {
        return singleVenue().getId().value();
    }

    private Venue findVenue(VenueId venueId) {
        return toDomain(venueRepository.findById(venueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VENUE_NOT_FOUND)));
    }

    private Venue singleVenue() {
        List<VenueDataValue> venues = venueRepository.findAll();
        if (venues.size() != 1) {
            throw new IllegalStateException("Expected exactly one venue, found " + venues.size());
        }
        return toDomain(venues.getFirst());
    }

    private Venue toDomain(VenueDataValue data) {
        return new Venue(VenueId.of(data.id()), data.name(), data.description(),
                data.address(), ZoneId.of(data.timezone()), data.openingHours());
    }
}

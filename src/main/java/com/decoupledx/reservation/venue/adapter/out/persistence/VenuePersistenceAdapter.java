package com.decoupledx.reservation.venue.adapter.out.persistence;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.venue.domain.model.DailyOpeningHours;
import com.decoupledx.reservation.venue.domain.model.OpeningHours;
import com.decoupledx.reservation.venue.domain.model.VenueId;
import com.decoupledx.reservation.venue.domain.model.Venue;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class VenuePersistenceAdapter implements VenueRepository {

    private final VenueJpaRepository venues;
    private final OpeningHoursJpaRepository openingHours;

    @Override
    public Optional<Venue> findById(VenueId id) {
        return venues.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Venue> findAll() {
        return venues.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Venue save(Venue venue) {
        venues.findById(venue.getId().value())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(venue.getName(), venue.getDescription(), venue.getAddress(),
                                venue.getTimezone().getId(), Instant.now()),
                        () -> venues.save(newVenueEntity(venue)));
        replaceOpeningHours(venue);
        return venue;
    }

    private VenueEntity newVenueEntity(Venue venue) {
        Instant now = Instant.now();
        return new VenueEntity(venue.getId().value(), venue.getName(), venue.getDescription(), venue.getAddress(),
                venue.getTimezone().getId(), now, now);
    }

    private void replaceOpeningHours(Venue venue) {
        openingHours.deleteByVenueId(venue.getId().value());
        List<OpeningHoursEntity> rows = venue.getOpeningHours().perDay().entrySet().stream()
                .map(entry -> new OpeningHoursEntity(
                        venue.getId().value(),
                        entry.getKey(),
                        entry.getValue().opensAt(),
                        entry.getValue().closesAt()))
                .toList();
        openingHours.saveAll(rows);
        openingHours.flush();
    }

    private Venue toDomain(VenueEntity entity) {
        Map<DayOfWeek, DailyOpeningHours> perDay = openingHours.findByVenueId(entity.getId()).stream()
                .collect(Collectors.toMap(
                        OpeningHoursEntity::getDayOfWeek,
                        row -> new DailyOpeningHours(row.getOpensAt(), row.getClosesAt())));
        return new Venue(
                VenueId.of(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getAddress(),
                ZoneId.of(entity.getTimezone()),
                new OpeningHours(perDay));
    }
}

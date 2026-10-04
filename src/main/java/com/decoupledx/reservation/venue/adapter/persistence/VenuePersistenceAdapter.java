package com.decoupledx.reservation.venue.adapter.persistence;

import com.decoupledx.reservation.venue.adapter.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class VenuePersistenceAdapter implements VenueRepository {

    private final VenueJpaRepository venues;
    private final OpeningHoursJpaRepository openingHours;

    @Override
    public Optional<VenueDataValue> findById(VenueId id) {
        return venues.findById(id.value()).map(this::toDataValue);
    }

    @Override
    public List<VenueDataValue> findAll() {
        return venues.findAll().stream().map(this::toDataValue).toList();
    }

    @Override
    public VenueDataValue save(VenueDataValue data) {
        venues.findById(data.id())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(data.name(), data.description(), data.address(),
                                data.timezone(), Instant.now()),
                        () -> venues.save(newVenueEntity(data)));
        replaceOpeningHours(data);
        return data;
    }

    private VenueEntity newVenueEntity(VenueDataValue data) {
        Instant now = Instant.now();
        return new VenueEntity(data.id(), data.name(), data.description(), data.address(),
                data.timezone(), now, now);
    }

    private void replaceOpeningHours(VenueDataValue data) {
        Map<DayOfWeek, OpeningHoursEntity> existingByDay = openingHours.findByVenueId(data.id()).stream()
                .collect(Collectors.toMap(OpeningHoursEntity::getDayOfWeek, entity -> entity));
        for (Map.Entry<DayOfWeek, DailyOpeningHours> entry : data.openingHours().perDay().entrySet()) {
            OpeningHoursEntity entity = existingByDay.remove(entry.getKey());
            if (entity != null) {
                entity.update(entry.getValue().opensAt(), entry.getValue().closesAt());
            } else {
                openingHours.save(new OpeningHoursEntity(
                        data.id(),
                        entry.getKey(),
                        entry.getValue().opensAt(),
                        entry.getValue().closesAt()));
            }
        }
        openingHours.deleteAll(existingByDay.values());
        openingHours.flush();
    }

    private VenueDataValue toDataValue(VenueEntity entity) {
        Map<DayOfWeek, DailyOpeningHours> perDay = openingHours.findByVenueId(entity.getId()).stream()
                .collect(Collectors.toMap(
                        OpeningHoursEntity::getDayOfWeek,
                        row -> new DailyOpeningHours(row.getOpensAt(), row.getClosesAt())));
        return new VenueDataValue(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAddress(),
                entity.getTimezone(),
                new OpeningHours(perDay));
    }
}

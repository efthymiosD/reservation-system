package com.decoupledx.reservation.policy.adapter.persistence;

import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BookingPolicyPersistenceAdapter implements BookingPolicyRepository {

    private final BookingPolicyJpaRepository bookingPolicies;

    @Override
    public Optional<BookingPolicyDataValue> findByVenueId(VenueId venueId) {
        return bookingPolicies.findById(venueId.value()).map(this::toDomain);
    }

    @Override
    public void save(VenueId venueId, BookingPolicyDataValue policy) {
        UUID id = venueId.value();
        bookingPolicies.findById(id).ifPresentOrElse(
                entity -> entity.updateFrom(
                        (int) policy.minDurationMinutes(),
                        (int) policy.maxDurationMinutes(),
                        (int) policy.durationStepMinutes(),
                        (int) policy.startTimeStepMinutes(),
                        policy.maxAdvanceBooking(),
                        Instant.now()),
                () -> bookingPolicies.save(new BookingPolicyEntity(
                        id,
                        (int) policy.minDurationMinutes(),
                        (int) policy.maxDurationMinutes(),
                        (int) policy.durationStepMinutes(),
                        (int) policy.startTimeStepMinutes(),
                        policy.maxAdvanceBooking(),
                        Instant.now())));
    }

    private BookingPolicyDataValue toDomain(BookingPolicyEntity entity) {
        return new BookingPolicyDataValue(
                entity.getMinDurationMinutes(),
                entity.getMaxDurationMinutes(),
                entity.getDurationStepMinutes(),
                entity.getStartTimeStepMinutes(),
                entity.getMaxAdvanceBooking());
    }
}

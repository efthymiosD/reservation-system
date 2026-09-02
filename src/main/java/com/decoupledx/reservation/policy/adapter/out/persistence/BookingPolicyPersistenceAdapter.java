package com.decoupledx.reservation.policy.adapter.out.persistence;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.policy.domain.model.BookingPolicy;
import com.decoupledx.reservation.policy.domain.port.BookingPolicyRepository;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class BookingPolicyPersistenceAdapter implements BookingPolicyRepository {

    private final BookingPolicyJpaRepository bookingPolicies;

    @Override
    public Optional<BookingPolicy> findByVenueId(VenueId venueId) {
        return bookingPolicies.findById(venueId.value()).map(this::toDomain);
    }

    @Override
    public void save(VenueId venueId, BookingPolicy policy) {
        UUID id = venueId.value();
        bookingPolicies.findById(id).ifPresentOrElse(
                entity -> entity.updateFrom(
                        toMinutes(policy.minDuration()),
                        toMinutes(policy.maxDuration()),
                        toMinutes(policy.durationStep()),
                        toMinutes(policy.startTimeStep()),
                        policy.maxAdvanceBooking().toString(),
                        Instant.now()),
                () -> bookingPolicies.save(new BookingPolicyEntity(
                        id,
                        toMinutes(policy.minDuration()),
                        toMinutes(policy.maxDuration()),
                        toMinutes(policy.durationStep()),
                        toMinutes(policy.startTimeStep()),
                        policy.maxAdvanceBooking().toString(),
                        Instant.now())));
    }

    private BookingPolicy toDomain(BookingPolicyEntity entity) {
        return new BookingPolicy(
                Duration.ofMinutes(entity.getMinDurationMinutes()),
                Duration.ofMinutes(entity.getMaxDurationMinutes()),
                Duration.ofMinutes(entity.getDurationStepMinutes()),
                Duration.ofMinutes(entity.getStartTimeStepMinutes()),
                Period.parse(entity.getMaxAdvanceBooking()));
    }

    private static int toMinutes(Duration duration) {
        return Math.toIntExact(duration.toMinutes());
    }
}

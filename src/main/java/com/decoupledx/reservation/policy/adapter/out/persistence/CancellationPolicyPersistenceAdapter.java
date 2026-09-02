package com.decoupledx.reservation.policy.adapter.out.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.policy.domain.model.CancellationPolicy;
import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CancellationPolicyPersistenceAdapter implements CancellationPolicyRepository {

    private final CancellationPolicyJpaRepository cancellationPolicies;

    @Override
    public Optional<CancellationPolicy> findByVenueId(VenueId venueId) {
        return cancellationPolicies.findById(venueId.value())
                .map(entity -> new CancellationPolicy(Duration.ofMinutes(entity.getDeadlineBeforeStartMinutes())));
    }

    @Override
    public void save(VenueId venueId, CancellationPolicy policy) {
        UUID id = venueId.value();
        int deadlineMinutes = Math.toIntExact(policy.deadlineBeforeStart().toMinutes());
        cancellationPolicies.findById(id).ifPresentOrElse(
                entity -> entity.updateFrom(deadlineMinutes, Instant.now()),
                () -> cancellationPolicies.save(new CancellationPolicyEntity(id, deadlineMinutes, Instant.now())));
    }
}

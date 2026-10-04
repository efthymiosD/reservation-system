package com.decoupledx.reservation.policy.adapter.persistence;

import com.decoupledx.reservation.policy.domain.port.CancellationPolicyRepository;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CancellationPolicyPersistenceAdapter implements CancellationPolicyRepository {

    private final CancellationPolicyJpaRepository cancellationPolicies;

    @Override
    public Optional<CancellationPolicyDataValue> findByVenueId(VenueId venueId) {
        return cancellationPolicies.findById(venueId.value()).map(this::toDomain);
    }

    @Override
    public void save(VenueId venueId, CancellationPolicyDataValue policy) {
        UUID id = venueId.value();
        cancellationPolicies.findById(id).ifPresentOrElse(
                entity -> entity.updateFrom((int) policy.deadlineBeforeStartMinutes(), Instant.now()),
                () -> cancellationPolicies.save(new CancellationPolicyEntity(
                        id, (int) policy.deadlineBeforeStartMinutes(), Instant.now())));
    }

    private CancellationPolicyDataValue toDomain(CancellationPolicyEntity entity) {
        return new CancellationPolicyDataValue(entity.getDeadlineBeforeStartMinutes());
    }
}

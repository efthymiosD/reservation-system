package com.decoupledx.reservation.pricing.adapter.persistence;

import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.shared.Money;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PricingPolicyPersistenceAdapter implements PricingPolicyRepository {

    private final PricingPolicyJpaRepository pricingPolicies;

    @Override
    public Optional<PricingPolicyDataValue> findByVenueId(VenueId venueId) {
        return pricingPolicies.findById(venueId.value())
                .map(entity -> new PricingPolicyDataValue(
                        entity.getVenueId(),
                        Money.of(entity.getHourlyPrice(), Currency.getInstance(entity.getCurrency()))));
    }

    @Override
    public void save(VenueId venueId, PricingPolicyDataValue policy) {
        UUID id = venueId.value();
        Money hourlyPrice = policy.hourlyPrice();
        pricingPolicies.findById(id).ifPresentOrElse(
                entity -> entity.updateFrom(hourlyPrice.amount(), hourlyPrice.currency().getCurrencyCode(),
                        Instant.now()),
                () -> pricingPolicies.save(new PricingPolicyEntity(
                        id, hourlyPrice.amount(), hourlyPrice.currency().getCurrencyCode(), Instant.now())));
    }
}

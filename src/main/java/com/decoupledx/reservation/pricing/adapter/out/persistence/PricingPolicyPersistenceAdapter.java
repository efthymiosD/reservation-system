package com.decoupledx.reservation.pricing.adapter.out.persistence;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.pricing.domain.model.PricingPolicy;
import com.decoupledx.reservation.pricing.domain.port.PricingPolicyRepository;
import com.decoupledx.reservation.shared.domain.Money;
import com.decoupledx.reservation.venue.domain.model.VenueId;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class PricingPolicyPersistenceAdapter implements PricingPolicyRepository {

    private final PricingPolicyJpaRepository pricingPolicies;

    @Override
    public Optional<PricingPolicy> findByVenueId(VenueId venueId) {
        return pricingPolicies.findById(venueId.value())
                .map(entity -> new PricingPolicy(
                        Money.of(entity.getHourlyPrice(), Currency.getInstance(entity.getCurrency()))));
    }

    @Override
    public void save(VenueId venueId, PricingPolicy policy) {
        UUID id = venueId.value();
        Money hourlyPrice = policy.hourlyPrice();
        pricingPolicies.findById(id).ifPresentOrElse(
                entity -> entity.updateFrom(hourlyPrice.amount(), hourlyPrice.currency().getCurrencyCode(),
                        Instant.now()),
                () -> pricingPolicies.save(new PricingPolicyEntity(
                        id, hourlyPrice.amount(), hourlyPrice.currency().getCurrencyCode(), Instant.now())));
    }
}

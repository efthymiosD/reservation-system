package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingPolicy;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Currency;

/**
 * Assembles the admin pricing page: reads the current hourly price and currency
 * from the pricing module and the venue profile via the module APIs. All
 * reduced API names live here; the page itself is a thin form over
 * {@link AdminPricingController}.
 */
@Component
@RequiredArgsConstructor
class AdminPricingFactory {

    private final PricingApi pricingApi;
    private final VenueApi venueApi;

    AdminPricingModel build() {
        VenueInfo venue = venueApi.getVenue(venueApi.singleVenueId());
        PricingPolicy policy = pricingApi.pricingPolicyFor(venueApi.singleVenueId());
        Currency currency = policy.hourlyPrice().currency();
        return new AdminPricingModel(
                currency.getCurrencyCode(),
                policy.hourlyPrice().amount(),
                venue.id().value());
    }
}

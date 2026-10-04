package com.decoupledx.reservation.pricing.adapter.web;

import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingPolicy;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.Money;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import jakarta.validation.Valid;
import java.util.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class PricingAdminController {

    private final PricingApi pricingService;
    private final VenueApi venueService;

    @GetMapping
    PricingGetResponse getPricing() {
        PricingPolicy policy = pricingService.pricingPolicyFor(venueService.singleVenueId());
        return new PricingGetResponse(
                policy.hourlyPrice().amount(),
                policy.hourlyPrice().currency().getCurrencyCode());
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updatePricing(@Valid @RequestBody PricingUpdateRequest request) {
        Currency currency;
        try {
            currency = Currency.getInstance(request.currency());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_PRICING_POLICY, "Unknown currency " + request.currency());
        }
        PricingPolicy policy = new PricingPolicy(Money.of(request.hourlyPrice(), currency));
        pricingService.updatePricingPolicy(venueService.singleVenueId(), policy);
    }

}

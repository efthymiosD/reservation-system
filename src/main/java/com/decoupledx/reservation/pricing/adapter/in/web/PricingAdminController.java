package com.decoupledx.reservation.pricing.adapter.in.web;

import java.math.BigDecimal;
import java.util.Currency;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.pricing.domain.model.PricingPolicy;
import com.decoupledx.reservation.pricing.domain.service.PricingService;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.Money;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class PricingAdminController {

    private final PricingService pricingService;
    private final VenueService venueService;

    @GetMapping
    PricingGetResponse getPricing() {
        PricingPolicy policy = pricingService.getPricingPolicy(venueService.singleVenueId());
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

    record PricingUpdateRequest(
            @NotNull @DecimalMin("0.0") BigDecimal hourlyPrice,
            @NotBlank String currency) {
    }

    record PricingGetResponse(BigDecimal hourlyPrice, String currency) {
    }
}

package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.pricing.adapter.api.PricingApi;
import com.decoupledx.reservation.pricing.adapter.api.PricingPolicy;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.Money;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * Admin pricing page (ROLE_ADMIN gated at the security chain): shows and edits
 * the venue's hourly rate and currency. The rate is expressed in Stripe-ready
 * minor units via {@link Money}; the page works entirely in major units (what
 * the admin types), and conversion to/from minor units happens in the shared
 * Money seam before it ever reaches a payment provider.
 *
 * <p>Changing the rate never rewrites reservation history: each reservation
 * carries its own price snapshot taken at booking time, so past invoices stay
 * immutable.
 */
@Controller
@RequiredArgsConstructor
class AdminPricingController {

    private final AdminPricingFactory pricingFactory;
    private final PricingApi pricingApi;
    private final VenueApi venueApi;

    @GetMapping("/admin/pricing")
    String pricing(Model model) {
        model.addAttribute("page", pricingFactory.build());
        return "admin/pricing";
    }

    @PostMapping("/admin/pricing")
    String updatePricing(
            @RequestParam BigDecimal hourlyPrice,
            @RequestParam String currency,
            RedirectAttributes redirect) {
        try {
            Currency parsed = Currency.getInstance(currency);
            UUID venueId = venueApi.singleVenueId();
            Money hourly = Money.of(hourlyPrice, parsed);
            pricingApi.updatePricingPolicy(venueId, new PricingPolicy(hourly));
            redirect.addFlashAttribute("message",
                    "Hourly rate updated to %s %s.".formatted(parsed, hourly.amount()));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", "A valid ISO-4217 currency code is required.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/admin/pricing";
    }

    private String userMessage(BusinessException exception) {
        if (Objects.requireNonNull(exception.errorCode()) == ErrorCode.INVALID_PRICING_POLICY) {
            return "The hourly price must be zero or positive.";
        }
        return exception.getMessage();
    }
}

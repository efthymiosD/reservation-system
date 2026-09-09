package com.decoupledx.reservation.webui.confirmation;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.decoupledx.reservation.identity.api.CurrentCustomerApi;
import com.decoupledx.reservation.shared.domain.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * Reservation confirmation page. Only the owning customer can view it; the
 * domain's ownership check drives the redirect for anyone else.
 */
@Controller
@RequiredArgsConstructor
class ConfirmationController {

    private final ConfirmationModelFactory confirmationModel;
    private final CurrentCustomerApi currentCustomer;

    @GetMapping("/reservations/{reservationId}/confirmation")
    String confirmation(@PathVariable UUID reservationId, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("confirmation", confirmationModel.build(
                    reservationId, currentCustomer.currentCustomerId()));
            return "confirmation";
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", "Reservation not found.");
            return "redirect:/reserve";
        }
    }
}

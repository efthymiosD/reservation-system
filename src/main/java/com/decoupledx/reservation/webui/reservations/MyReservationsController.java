package com.decoupledx.reservation.webui.reservations;

import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.shared.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The customer's reservation list and cancellation. Only the owning customer
 * reaches the page (the reservation module's ownership checks drive redirects),
 * and the cancel use case revalidates the cancellation deadline server-side —
 * the page's Cancel button is a UX hint only.
 */
@Controller
@RequiredArgsConstructor
class MyReservationsController {

    private final MyReservationsModelFactory modelFactory;
    private final ReservationApi reservationApi;

    @GetMapping("/my-reservations")
    String myReservations(Model model) {
        model.addAttribute("page", modelFactory.build());
        return "my-reservations";
    }

    @PostMapping("/my-reservations/{reservationId}/cancel")
    String cancel(@PathVariable UUID reservationId, RedirectAttributes redirect) {
        try {
            reservationApi.cancel(reservationId);
            redirect.addFlashAttribute("message", "Your reservation has been cancelled.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/my-reservations";
    }

    private String userMessage(BusinessException exception) {
        return switch (exception.errorCode()) {
            case CANCELLATION_DEADLINE_PASSED -> "The cancellation deadline has passed — the booking can no "
                    + "longer be cancelled.";
            case RESERVATION_ALREADY_CANCELLED -> "This reservation is already cancelled.";
            default -> exception.getMessage();
        };
    }
}

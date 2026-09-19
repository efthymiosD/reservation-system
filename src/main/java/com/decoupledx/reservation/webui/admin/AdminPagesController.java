package com.decoupledx.reservation.webui.admin;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.shared.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * Admin panel pages (ROLE_ADMIN gated at the security chain): dashboard and the
 * site-wide reservations overview. Cancelling a customer's reservation is
 * explicit admin override semantics — it bypasses the cancellation deadline by
 * design. All actions delegate to module APIs and are revalidated there.
 */
@Controller
@RequiredArgsConstructor
class AdminPagesController {

    private final AdminDashboardFactory dashboardFactory;
    private final AdminReservationsFactory reservationsFactory;
    private final ReservationApi reservationApi;
    private final CurrentCustomerApi currentCustomer;

    @GetMapping("/admin")
    String dashboard(Model model) {
        model.addAttribute("page", dashboardFactory.build());
        return "admin/dashboard";
    }

    @GetMapping("/admin/reservations")
    String reservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("page", reservationsFactory.build(status, page, size));
        return "admin/reservations";
    }

    @PostMapping("/admin/reservations/{reservationId}/cancel")
    String cancel(@PathVariable UUID reservationId, RedirectAttributes redirect) {
        try {
            reservationApi.cancelAdministratively(
                    reservationId, currentCustomer.currentCustomerId());
            redirect.addFlashAttribute("message", "Reservation " + shortRef(reservationId) + " cancelled (admin override).");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/admin/reservations";
    }

    private String shortRef(UUID reservationId) {
        return reservationId.toString().substring(0, 8);
    }

    private String userMessage(BusinessException exception) {
        return switch (exception.errorCode()) {
            case RESERVATION_NOT_FOUND -> "That reservation does not exist.";
            case RESERVATION_ALREADY_CANCELLED -> "This reservation is already cancelled.";
            case RESERVATION_IN_PAST -> "That reservation is in the past and can no longer be cancelled.";
            default -> exception.getMessage();
        };
    }
}

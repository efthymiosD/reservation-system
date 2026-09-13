package com.decoupledx.reservation.webui.admin;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.decoupledx.reservation.administration.api.AdministrationApi;
import com.decoupledx.reservation.administration.api.CreateRecurringReservationCommand;
import com.decoupledx.reservation.administration.api.RecurringReservationMaterializationSummary;
import com.decoupledx.reservation.identity.api.CurrentCustomerApi;
import com.decoupledx.reservation.shared.domain.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * Admin recurring-reservations page (ROLE_ADMIN gated at the security chain):
 * assigns a customer a recurring weekly slot on a resource, lists recurring
 * reservations with their status, cancels whole recurring reservations, and
 * triggers a manual materialization run. Start time and duration are chosen like
 * on the customer reserve page; the end time is derived from them. Creation
 * materializes the first batch of occurrences immediately; every action is
 * revalidated server-side through the administration module API.
 */
@Controller
@RequiredArgsConstructor
class AdminRecurringReservationsController {

    private final AdminRecurringReservationsFactory recurringReservationsFactory;
    private final AdministrationApi administrationApi;
    private final CurrentCustomerApi currentCustomer;

    @GetMapping("/admin/recurring-reservations")
    String recurringReservations(Model model) {
        model.addAttribute("page", recurringReservationsFactory.build());
        return "admin/recurring-reservations";
    }

    @PostMapping("/admin/recurring-reservations")
    String createRecurringReservation(
            @RequestParam UUID resourceId,
            @RequestParam UUID customerId,
            @RequestParam String weekday,
            @RequestParam LocalTime startTime,
            @RequestParam Integer durationMinutes,
            @RequestParam Integer windowMonths,
            RedirectAttributes redirect) {
        try {
            CreateRecurringReservationCommand command = new CreateRecurringReservationCommand(
                    resourceId, customerId, DayOfWeek.valueOf(weekday), startTime,
                    startTime.plusMinutes(durationMinutes), windowMonths);
            administrationApi.createRecurringReservation(command);
            redirect.addFlashAttribute("message", "Recurring reservation created — upcoming occurrences were booked.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/admin/recurring-reservations";
    }

    @PostMapping("/admin/recurring-reservations/{recurringReservationId}/cancel")
    String cancelRecurringReservation(@PathVariable UUID recurringReservationId, RedirectAttributes redirect) {
        try {
            administrationApi.cancelRecurringReservation(recurringReservationId, currentCustomer.currentCustomerId());
            redirect.addFlashAttribute("message",
                    "Recurring reservation " + shortRef(recurringReservationId) + " cancelled (future bookings released).");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/admin/recurring-reservations";
    }

    @PostMapping("/admin/recurring-reservations/materialize")
    String materialize(RedirectAttributes redirect) {
        RecurringReservationMaterializationSummary summary = administrationApi.materializeDue();
        redirect.addFlashAttribute("message",
                "Materialization done — %d reservation(s) created, %d skipped."
                        .formatted(summary.created(), summary.skipped()));
        return "redirect:/admin/recurring-reservations";
    }

    private String shortRef(UUID recurringReservationId) {
        return recurringReservationId.toString().substring(0, 8);
    }

    private String userMessage(BusinessException exception) {
        return switch (exception.errorCode()) {
            case RECURRING_RESERVATION_NOT_FOUND -> "That recurring reservation does not exist.";
            case RECURRING_RESERVATION_ALREADY_CANCELLED -> "This recurring reservation is already cancelled.";
            case INVALID_RECURRING_RESERVATION_PERIOD -> "The end time must be after the start time.";
            case INVALID_RECURRING_RESERVATION_WINDOW -> "The booking window must be 1, 3 or 6 months.";
            case RESOURCE_NOT_FOUND -> "That resource does not exist.";
            case RESOURCE_INACTIVE -> "That resource is inactive.";
            case OUTSIDE_OPENING_HOURS -> "The slot does not fit within the venue's opening hours on that weekday.";
            case INVALID_RESERVATION_DURATION -> "The slot duration does not match the venue booking policy.";
            default -> exception.getMessage();
        };
    }
}
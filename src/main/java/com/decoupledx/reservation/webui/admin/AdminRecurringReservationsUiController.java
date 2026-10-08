package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.administration.adapter.api.AdministrationApi;
import com.decoupledx.reservation.administration.adapter.api.CreateRecurringReservationCommand;
import com.decoupledx.reservation.administration.adapter.api.RecurringReservationMaterializationSummary;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.webui.WebMessages;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
class AdminRecurringReservationsUiController {

    private final AdminRecurringReservationsFactory recurringReservationsFactory;
    private final AdministrationApi administrationApi;
    private final WebMessages messages;

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
            redirect.addFlashAttribute("message",
                    messages.get("admin.recurringCreated"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/admin/recurring-reservations";
    }

    @PostMapping("/admin/recurring-reservations/{recurringReservationId}/cancel")
    String cancelRecurringReservation(@PathVariable UUID recurringReservationId, RedirectAttributes redirect) {
        try {
            administrationApi.cancelRecurringReservation(recurringReservationId);
            redirect.addFlashAttribute("message",
                    messages.get("admin.recurringCancelled", shortRef(recurringReservationId)));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/admin/recurring-reservations";
    }

    @PostMapping("/admin/recurring-reservations/materialize")
    String materialize(RedirectAttributes redirect) {
        RecurringReservationMaterializationSummary summary = administrationApi.materializeDue();
        redirect.addFlashAttribute("message", messages.get(
                "admin.materializationDone", summary.created(), summary.skipped()));
        return "redirect:/admin/recurring-reservations";
    }

    private String shortRef(UUID recurringReservationId) {
        return recurringReservationId.toString().substring(0, 8);
    }
}
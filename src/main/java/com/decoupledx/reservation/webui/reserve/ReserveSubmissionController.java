package com.decoupledx.reservation.webui.reserve;

import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.webui.WebMessages;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Submits the reservation from the reservation page. Success redirects to the
 * confirmation page; business rejections redirect back to the availability map
 * with a friendly message (the map then renders freshly computed state, which is
 * what makes stale availability safe).
 */
@Controller
@RequiredArgsConstructor
class ReserveSubmissionController {

    private final ReserveSubmissionService submission;
    private final WebMessages messages;

    @PostMapping("/reserve")
    String reserve(@RequestParam UUID resourceId,
                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
                   @RequestParam int durationMinutes,
                   RedirectAttributes redirect) {
        try {
            ReservationInfo created = submission.submit(resourceId, date, start, durationMinutes);
            redirect.addAttribute("reservationId", created.id().value());
            return "redirect:/reservations/{reservationId}/confirmation";
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            return "redirect:/reserve?date=%s&start=%s&durationMinutes=%d"
                    .formatted(date, start, durationMinutes);
        }
    }
}

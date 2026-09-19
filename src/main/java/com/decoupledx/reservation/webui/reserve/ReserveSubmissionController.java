package com.decoupledx.reservation.webui.reserve;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.decoupledx.reservation.identity.adapter.api.CurrentCustomerApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.shared.BusinessException;

import lombok.RequiredArgsConstructor;

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
    private final CurrentCustomerApi currentCustomer;

    @PostMapping("/reserve")
    String reserve(@RequestParam UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam int durationMinutes,
            RedirectAttributes redirect) {
        try {
            ReservationInfo created = submission.submit(
                    resourceId, date, start, durationMinutes, currentCustomer.currentCustomerId());
            redirect.addAttribute("reservationId", created.id().value());
            return "redirect:/reservations/{reservationId}/confirmation";
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
            return "redirect:/reserve?date=%s&start=%s&durationMinutes=%d"
                    .formatted(date, start, durationMinutes);
        }
    }

    private String userMessage(BusinessException exception) {
        return switch (exception.errorCode()) {
            case RESOURCE_NO_LONGER_AVAILABLE -> "The selected field is no longer available. "
                    + "The availability has been refreshed — please select another field or time.";
            case CUSTOMER_HAS_OVERLAPPING_RESERVATION -> "You already have a reservation overlapping "
                    + "this time.";
            case INVALID_RESERVATION_DURATION -> "That duration is not offered. "
                    + "Please choose one of the listed durations.";
            case INVALID_START_TIME -> "That start time is not bookable. "
                    + "Please choose one of the listed start times.";
            case START_TIME_IN_PAST -> "That start time is in the past. Please pick an upcoming slot.";
            case OUTSIDE_OPENING_HOURS -> "The venue is closed at the selected time. "
                    + "Please pick a slot within the opening hours.";
            case ADVANCE_BOOKING_LIMIT_EXCEEDED -> "That date is too far in the future. "
                    + "Please pick a date within the booking window.";
            default -> exception.getMessage();
        };
    }
}

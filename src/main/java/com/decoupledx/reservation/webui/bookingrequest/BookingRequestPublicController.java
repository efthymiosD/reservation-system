package com.decoupledx.reservation.webui.bookingrequest;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestApi;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.SubmitBookingRequestCommand;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.webui.WebMessages;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * The PUBLIC booking-request flow: a customer opens the coach's link on a
 * phone, picks slot values (advisory only), and submits; the request lands in
 * the module's queue as PENDING — nothing is reserved. No management action
 * is reachable from here; the coach works in the authenticated admin UI.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
class BookingRequestPublicController {

    private final BookingRequestApi bookingRequests;
    private final BookingRequestFormFactory formFactory;
    private final WebMessages messages;

    @GetMapping("/booking-requests/new/{token}")
    String form(@PathVariable String token, Model model, HttpServletResponse response) {
        noStore(response);
        if (!linkResolves(token)) {
            return "redirect:/booking-requests/invalid";
        }
        model.addAttribute("page", new BookingRequestFormView(
                formFactory.options(), token, SubmissionKeys.newKey()));
        return "bookingrequest/form";
    }

    @PostMapping("/booking-requests/new/{token}")
    String submit(@PathVariable String token,
                  @Valid @ModelAttribute BookingRequestSubmission submission,
                  BindingResult binding,
                  RedirectAttributes redirect) {
        if (!SubmissionKeys.wellFormed(submission.submissionKey())) {
            // A tampered/foreign visit: treated as a plain form render.
            return "redirect:/booking-requests/new/" + token;
        }
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", firstFieldError(binding));
            redirect.addFlashAttribute("submission", submission);
            return "redirect:/booking-requests/new/" + token;
        }
        try {
            UUID linkId = bookingRequests.resolveLink(token);
            bookingRequests.submit(linkId, commandOf(submission));
            return "redirect:/booking-requests/new/" + token + "/ack/" + submission.submissionKey();
        } catch (BusinessException businessFailure) {
            log.info("Booking request submit failed: {}", businessFailure.errorCode());
            redirect.addFlashAttribute("error", messages.errorMessage(businessFailure));
            return "redirect:/booking-requests/new/" + token;
        }
    }

    @GetMapping("/booking-requests/new/{token}/ack/{submissionKey}")
    String acknowledgement(@PathVariable String token, @PathVariable String submissionKey,
                           Model model, HttpServletResponse response) {
        noStore(response);
        // Single resolution: the lookup itself resolves the link (and matches
        // the request against it) — unknown token, revoked link or a foreign
        // submission key all end on the same friendly page.
        Optional<BookingRequestInfo> request = bookingRequests.bySubmissionKey(token, submissionKey);
        if (request.isEmpty()) {
            return "redirect:/booking-requests/invalid";
        }
        model.addAttribute("page", request.get());
        return "bookingrequest/ack";
    }

    @GetMapping("/booking-requests/invalid")
    String invalidLink(Model model, HttpServletResponse response) {
        noStore(response);
        return "bookingrequest/invalid";
    }

    /**
     * The link pages mint derive-what-you-see from the CURRENT clock (min
     * dates, options, statuses). A cached copy from yesterday offers already
     * past days — always fetch fresh.
     */
    private static void noStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    private boolean linkResolves(String token) {
        try {
            bookingRequests.resolveLink(token);
            return true;
        } catch (BusinessException unknownOrRevoked) {
            log.info("Booking-request link rejected: {}", unknownOrRevoked.errorCode());
            return false;
        }
    }

    private SubmitBookingRequestCommand commandOf(BookingRequestSubmission submission) {
        return new SubmitBookingRequestCommand(
                submission.resourceId(),
                LocalDateTime.of(submission.date(), submission.start()),
                submission.durationMinutes(),
                submission.customerName(),
                submission.contact(),
                submission.note(),
                submission.submissionKey());
    }

    private String firstFieldError(BindingResult binding) {
        return binding.getAllErrors().iterator().next().getDefaultMessage();
    }
}

package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestApi;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkToken;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.webui.WebMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Coach/ADMIN management of booking requests (ROLE_ADMIN gated at the
 * security chain): the queue view + the approve/reject decisions + the
 * shareable WhatsApp-link lifecycle. Approving a request is an
 * ADMINISTRATIVE staff placement (the module revalidates everything).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
class BookingRequestsAdminController {

    private final BookingRequestApi bookingRequests;
    private final BookingRequestsFactory requestsFactory;
    private final WebMessages messages;

    @GetMapping("/admin/booking-requests")
    String list(Model model) {
        model.addAttribute("page", requestsFactory.build());
        return "admin/booking-requests";
    }

    @PostMapping("/admin/booking-requests/{requestId}/approve")
    String approve(@PathVariable UUID requestId, RedirectAttributes redirect) {
        try {
            BookingRequestInfo decided = bookingRequests.approve(requestId);
            redirect.addFlashAttribute("message", messages.get(
                    "admin.bookingRequests.approved", decided.reference()));
        } catch (BusinessException failure) {
            redirect.addFlashAttribute("error", messages.errorMessage(failure));
        }
        return "redirect:/admin/booking-requests";
    }

    @PostMapping("/admin/booking-requests/{requestId}/reject")
    String reject(@PathVariable UUID requestId, RedirectAttributes redirect) {
        try {
            BookingRequestInfo decided = bookingRequests.reject(requestId);
            redirect.addFlashAttribute("message", messages.get(
                    "admin.bookingRequests.rejected", decided.reference()));
        } catch (BusinessException failure) {
            redirect.addFlashAttribute("error", messages.errorMessage(failure));
        }
        return "redirect:/admin/booking-requests";
    }

    // The share-link base: a configured public base wins (dev funnel / prod
    // domain); empty falls back to the coach's own browsing host — which only
    // works when they browse via the public door.
    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @PostMapping("/admin/booking-requests/link")
    String regenerateLink(RedirectAttributes redirect) {
        try {
            BookingRequestLinkToken token = bookingRequests.issueNewLink(null);
            redirect.addFlashAttribute("freshShareLink", shareLinkFor(token));
            redirect.addFlashAttribute("message", messages.get("admin.bookingRequests.linkIssued"));
        } catch (BusinessException raceLostToo) {
            redirect.addFlashAttribute("error", messages.errorMessage(raceLostToo));
        }
        return "redirect:/admin/booking-requests";
    }

    private String shareLinkFor(BookingRequestLinkToken token) {
        String base = publicBaseUrl != null && !publicBaseUrl.isBlank()
                ? trimTrailingSlash(publicBaseUrl)
                : ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return base + "/booking-requests/new/" + token.value();
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @PostMapping("/admin/booking-requests/link/revoke")
    String revokeLink(RedirectAttributes redirect) {
        bookingRequests.revokeActiveLink();
        redirect.addFlashAttribute("message", messages.get("admin.bookingRequests.linkRevoked"));
        return "redirect:/admin/booking-requests";
    }
}

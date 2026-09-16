package com.decoupledx.reservation.webui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public venue pages (about, contact). All venue data reaches the views through
 * VenueAdvice; the controllers only select templates. The opening-hours page was
 * merged into the contact page; /opening-hours redirects there.
 */
@Controller
class VenuePagesController {

    @GetMapping("/about")
    String about() {
        return "about";
    }

    @GetMapping("/contact")
    String contact() {
        return "contact";
    }

    @GetMapping("/opening-hours")
    String openingHours() {
        return "redirect:/contact";
    }
}

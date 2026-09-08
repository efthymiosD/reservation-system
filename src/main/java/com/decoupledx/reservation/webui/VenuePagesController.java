package com.decoupledx.reservation.webui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public venue pages (about, opening hours, contact). All venue data reaches the
 * views through VenueAdvice; the controllers only select templates.
 */
@Controller
class VenuePagesController {

    @GetMapping("/about")
    String about() {
        return "about";
    }

    @GetMapping("/opening-hours")
    String openingHours() {
        return "opening-hours";
    }

    @GetMapping("/contact")
    String contact() {
        return "contact";
    }
}

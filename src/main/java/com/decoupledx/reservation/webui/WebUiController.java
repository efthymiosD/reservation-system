package com.decoupledx.reservation.webui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public web pages of the reservation web UI. Serves the home page; further public
 * pages (about/venue, opening hours, contact) arrive with the public-pages task and
 * the reservation page arrives with the reservation-slice task.
 */
@Controller
class WebUiController {

    @GetMapping("/")
    String home() {
        return "home";
    }
}

package com.decoupledx.reservation.webui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing page after a successful single logout (Keycloak redirects back here).
 * Public by definition — the session was just terminated.
 */
@Controller
class LoggedOutController {

    @GetMapping("/logged-out")
    String loggedOut() {
        return "logged-out";
    }
}

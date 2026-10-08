package com.decoupledx.reservation.webui;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the current request path to every web UI view. The navigation
 * language selector posts the ?lang= switch back to the page the user is on,
 * so the choice is applied without losing context.
 *
 * <p>Only the path is used, never the query string — a stale bind/filter
 * parameter must not be replayed when the form resubmits.</p>
 */
@ControllerAdvice
class CurrentPathAdvice {

    @ModelAttribute("currentPath")
    String currentPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.isEmpty() ? "/" : path;
    }
}

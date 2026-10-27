package com.decoupledx.reservation.webui.bookingrequest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic anti-spam guard for the PUBLIC booking-request submission: a simple
 * per-client fixed-window throttle in memory. Single-node deployment (QNAP
 * DEV) — no distributed store is warranted; restarting the app purges
 * counters. Only POST submissions are tracked (the form GET stays cheap).
 *
 * <p>Honest limits: the client key is the FIRST X-Forwarded-For entry when
 * present, else the socket address. X-Forwarded-For is client-controllable,
 * so this is a nuisance guard, not an enforcement boundary — the real
 * boundaries remain the link token, the dedupe key and server-side
 * validation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class BookingRequestRateLimitFilter extends GenericFilterBean {

    /** Max POST submissions per client key inside one window. */
    static final int MAX_SUBMISSIONS_PER_WINDOW = 6;
    static final Duration WINDOW = Duration.ofMinutes(10);
    /** Map bound: a spoofed-header flood cannot grow the tracker without limit. */
    private static final int MAX_TRACKED_CLIENTS = 5_000;

    private final Map<String, Deque<Long>> submissionsByClient = new ConcurrentHashMap<>();

    /**
     * Test-infra hook: the singleton's window state survives the cached
     * Spring context between test classes.
     */
    public void reset() {
        submissionsByClient.clear();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest http
                && HttpMethod.POST.matches(http.getMethod())
                && http.getRequestURI().startsWith("/booking-requests/new/")) {
            if (throttleExceeded(clientKey(http))) {
                respondThrottled(response);
                return;
            }
        }
        chain.doFilter(servletRequest, response);
    }

    private boolean throttleExceeded(String clientKey) {
        long now = System.currentTimeMillis();
        if (submissionsByClient.size() >= MAX_TRACKED_CLIENTS) {
            // Crude but bounded: drop the whole window when the map is at its
            // cap (worst case: one window of grace for the flood).
            submissionsByClient.clear();
        }
        Deque<Long> stamps = submissionsByClient.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());
        synchronized (stamps) {
            while (!stamps.isEmpty() && now - stamps.peekFirst() > WINDOW.toMillis()) {
                stamps.removeFirst();
            }
            if (stamps.size() >= MAX_SUBMISSIONS_PER_WINDOW) {
                return true;
            }
            stamps.addLast(now);
            return false;
        }
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private void respondThrottled(ServletResponse response) throws IOException {
        HttpServletResponse http = (HttpServletResponse) response;
        http.setStatus(429);
        http.setContentType("text/plain;charset=UTF-8");
        http.getWriter().write("Too many requests — please wait a few minutes and try again.");
    }
}

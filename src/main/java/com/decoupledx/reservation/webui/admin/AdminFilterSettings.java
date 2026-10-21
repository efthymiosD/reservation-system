package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Admin-panel filter limits. Admin-controlled via the site-content admin page:
 * the key {@value #MAX_RANGE_DAYS_KEY} holds the maximum width of the admin
 * reservations date-range window in days (1..366). An absent, blank, or
 * unparseable key falls back to {@value #DEFAULT_MAX_RANGE_DAYS} — the ship
 * behaviour.
 */
@Component
@RequiredArgsConstructor
public class AdminFilterSettings {

    public static final String MAX_RANGE_DAYS_KEY = "admin.filter.max-range-days";
    public static final int DEFAULT_MAX_RANGE_DAYS = 90;
    private static final int HARD_MINIMUM_DAYS = 1;
    private static final int HARD_MAXIMUM_DAYS = 366;

    private final ContentApi contentApi;

    public int maxRangeDays() {
        Integer parsed = parseDays(contentApi.get(MAX_RANGE_DAYS_KEY));
        return parsed == null ? DEFAULT_MAX_RANGE_DAYS : parsed;
    }

    /** The admin-authored value as days, or {@code null} when blank/garbage/out of bounds. */
    static Integer parseDays(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int days = Integer.parseInt(raw.trim());
            if (days >= HARD_MINIMUM_DAYS && days <= HARD_MAXIMUM_DAYS) {
                return days;
            }
        } catch (NumberFormatException notANumber) {
            // fall through
        }
        return null;
    }
}

package com.decoupledx.reservation.webui;

import com.decoupledx.reservation.shared.BusinessException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single entry point for user-facing strings generated on the server side
 * (flash messages, model-factory labels, error-code explanations). Keys come
 * from the shared bundles (src/main/resources/messages); the locale is the
 * request's resolved locale.
 *
 * <p>Business error codes map to {@code error.<code>} keys — the domain stays
 * language-agnostic and the web layer explains codes in the user's language.</p>
 */
@Component
@RequiredArgsConstructor
public class WebMessages {

    /** Generated field codes are stable internal identifiers, not UI copy. */
    private static final String FIELD_CODE_PATTERN = "FIELD-%02d";

    private final MessageSource messageSource;

    /** Plain key lookup ({@code 1 h}-style fragments and static labels). */
    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, currentLocale());
    }

    /**
     * Explanation for a business exception in the user's language; unknown
     * codes fall back to the exception's own message.
     */
    public String errorMessage(BusinessException exception) {
        String key = "error." + exception.errorCode().name().toLowerCase();
        return messageSource.getMessage(key, null, exception.getMessage(), currentLocale());
    }

    public String fieldCode(int next) {
        return FIELD_CODE_PATTERN.formatted(next);
    }

    /** Weekday display name shared by admin tables and opening-hours forms. */
    public String weekday(java.time.DayOfWeek day) {
        return get("day." + day.name().toLowerCase());
    }

    /** "{d} h" / "{d} h {m} min" localized duration fragment. */
    public String duration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        String text = get("ui.duration.hours", hours);
        return rest > 0 ? text + get("ui.duration.minutes", rest) : text;
    }

    private static Locale currentLocale() {
        return LocaleContextHolder.getLocale();
    }
}

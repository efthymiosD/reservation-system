package com.decoupledx.reservation.webui;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Which languages are currently enabled for visitors. Admin-controlled: the
 * "Languages" card on the site-content admin page writes the site content key
 * {@value #ENABLED_LOCALES_KEY} as a CSV of language tags. An empty or missing
 * key means "every supported language is enabled" (ship behaviour). The
 * default language can never be disabled — it is the final fallback.
 */
@Component
@RequiredArgsConstructor
public class EnabledLocales {

    public static final String ENABLED_LOCALES_KEY = "ui.locales.enabled";

    private final ContentApi contentApi;

    /** Enabled order, in the display order of {@link SupportedLocales#ALL}. */
    public List<Locale> active() {
        String csv = contentApi.get(ENABLED_LOCALES_KEY);
        if (csv == null || csv.isBlank()) {
            return SupportedLocales.ALL;
        }
        List<String> enabled = new ArrayList<>(List.of(csv.toLowerCase().split(",")));
        enabled.retainAll(SupportedLocales.ALL.stream().map(Locale::getLanguage).toList());
        enabled.add(SupportedLocales.DEFAULT.getLanguage()); // default language is always on
        List<Locale> result = new ArrayList<>();
        SupportedLocales.ALL.forEach(locale -> {
            if (enabled.contains(locale.getLanguage())) {
                result.add(locale);
            }
        });
        return result;
    }

    public boolean isEnabled(Locale locale) {
        return locale != null
                && active().stream().anyMatch(candidate -> candidate.getLanguage().equals(locale.getLanguage()));
    }

    /** Raw admin-authored CSV, used by the Languages card to restore switches. */
    public List<String> authoredTags() {
        String csv = contentApi.get(ENABLED_LOCALES_KEY);
        return csv == null || csv.isBlank()
                ? List.of()
                : List.of(csv.toLowerCase().split(","));
    }
}

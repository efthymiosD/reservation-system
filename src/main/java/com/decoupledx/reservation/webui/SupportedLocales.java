package com.decoupledx.reservation.webui;

import java.util.List;
import java.util.Locale;

/**
 * The locales the UI actually ships translations for. Adding a language
 * means: a messages_&lt;tag&gt;.properties bundle, a new entry here, and a
 * new option in the navigation language selector (fragments/navigation.html).
 */
public final class SupportedLocales {

    public static final Locale DEFAULT = Locale.forLanguageTag("en");
    /** Display order: English, Polish, Greek. */
    public static final List<Locale> ALL =
            List.of(Locale.forLanguageTag("en"), Locale.forLanguageTag("pl"), Locale.forLanguageTag("el"));

    private SupportedLocales() {}

    /** Language tags of {@link #ALL}, lowercase, in display order ("en", "pl", "el"). */
    public static List<String> tags() {
        return ALL.stream().map(Locale::getLanguage).toList();
    }

    /** Locale for a lowercase, lowercase-only language tag in {@link #ALL}. */
    public static Locale of(String language) {
        return ALL.stream()
                .filter(locale -> locale.getLanguage().equals(language))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported language: " + language));
    }
}

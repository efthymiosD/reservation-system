package com.decoupledx.reservation.webui;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import com.decoupledx.reservation.content.adapter.api.SiteContentBlock;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes admin-authored site content to all views, localized per request.
 *
 * <p>Every text block has per-language, admin-authored fields: the master key
 * ({@code key}, English — the default language) and optional locale-qualified
 * siblings ({@code pl:key}, {@code el:key}) written through the admin panel
 * under the language chosen in {@code ?contentLang=}. Per language it renders
 * exactly what that language's author wrote; when a language has no authored
 * copy, the key is left out of the model entirely so templates fall through
 * to the built-in bundle default of that language. Authored copy is never
 * mixed across languages, never translated, and never reshaped by the
 * system.</p>
 */
@ControllerAdvice
@RequiredArgsConstructor
class ContentAdvice {

    private final ContentApi contentApi;

    @ModelAttribute("siteContent")
    Map<String, String> content() {
        Map<String, String> authored = new HashMap<>();
        for (SiteContentBlock block : contentApi.all()) {
            authored.put(block.key(), block.body());
        }
        String language = LocaleContextHolder.getLocale().getLanguage();
        Map<String, String> effective = new HashMap<>();
        for (Map.Entry<String, String> master : authored.entrySet()) {
            String key = master.getKey();
            if (isLocaleQualifiedKey(key)) {
                continue;
            }
            String value = SupportedLocales.DEFAULT.getLanguage().equals(language)
                    ? master.getValue()
                    : authored.get(language + ":" + key);
            if (value != null) {
                effective.put(key, value);
            }
        }
        return effective;
    }

    /** "pl:home.feature.1.title" is locale-qualified; "venue.map.layout" is not. */
    static boolean isLocaleQualifiedKey(String key) {
        return key.matches("[a-z]{2}:[\\S]+");
    }
}

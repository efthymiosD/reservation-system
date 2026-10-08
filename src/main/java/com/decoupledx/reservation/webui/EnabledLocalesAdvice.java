package com.decoupledx.reservation.webui;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the languages the navigation language selector offers: only the
 * locales the admin currently has enabled for visitors.
 */
@ControllerAdvice
@RequiredArgsConstructor
class EnabledLocalesAdvice {

    private final EnabledLocales enabledLocales;

    @ModelAttribute("enabledLocales")
    List<Locale> enabledLocales() {
        return enabledLocales.active();
    }
}

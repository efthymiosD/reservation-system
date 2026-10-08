package com.decoupledx.reservation.webui;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Locale resolution for the server-rendered UI:
 *
 * <ol>
 *   <li>explicit user choice persisted in the "locale" cookie (set via
 *       {@code ?lang=<tag>} through the LocaleChangeInterceptor),</li>
 *   <li>otherwise the browser's Accept-Language header,</li>
 *   <li>otherwise the default language (English).</li>
 * </ol>
 *
 * <p>Every resolved locale is constrained to the languages the admin currently
 * {@link EnabledLocales enables} for visitors: a stale cookie, a foreign
 * client value or a just-disabled language silently falls back to the next
 * supported option instead of rendering an odd hybrid. Languages ship enabled;
 * the admin can turn any non-default language off from the site-content page.</p>
 */
@Configuration
class LocaleConfig implements WebMvcConfigurer {

    /** One year — the explicit choice is meant to survive browser restarts. */
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final EnabledLocales enabledLocales;

    LocaleConfig(EnabledLocales enabledLocales) {
        this.enabledLocales = enabledLocales;
    }

    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("locale") {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                return sanitize(super.resolveLocale(request), request);
            }
        };
        // No-cookie default function: negotiate the browser's Accept-Language
        // against the supported set (spring-webmvc's stock function would
        // either stop at the configured default or ignore the header).
        resolver.setDefaultLocaleFunction(request ->
                sanitize(request.getLocale(), request));
        resolver.setCookieMaxAge(COOKIE_MAX_AGE);
        resolver.setCookieHttpOnly(false); // the UI reads it for selector state
        return resolver;
    }

    /** Out-of-set or disabled locales fall back to the default language. */
    private Locale sanitize(Locale candidate, HttpServletRequest request) {
        if (candidate == null) {
            return SupportedLocales.DEFAULT;
        }
        boolean supported = SupportedLocales.ALL.stream()
                .anyMatch(shipped -> shipped.getLanguage().equals(candidate.getLanguage()));
        if (supported && enabledLocales.isEnabled(candidate)) {
            return candidate;
        }
        Locale requested = request.getLocale();
        if (requested != null
                && !requested.getLanguage().equals(candidate.getLanguage())
                && SupportedLocales.ALL.stream()
                        .anyMatch(shipped -> shipped.getLanguage().equals(requested.getLanguage()))
                && enabledLocales.isEnabled(requested)) {
            return requested;
        }
        return SupportedLocales.DEFAULT;
    }

    @Bean
    LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}

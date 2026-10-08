package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webAdmin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * i18n contract: locale plumbing (cookie + Accept-Language + ?lang switch),
 * bundle parity across the supported locales, and localized rendering of the
 * customer surface plus the admin panel. Mechanism only — the tests observe
 * the {@code html lang} attribute and unresolved-key markers, never concrete
 * translation values (copy changes must not break plumbing tests).
 */
@AutoConfigureMockMvc
class I18nIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageSource messageSource;

    @Test
    void explicitLangParamSwitchesLocaleAndSetsCookie() throws Exception {
        String setCookie = mockMvc.perform(get("/").param("lang", "pl"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).as("?lang must persist the choice").startsWith("locale=pl;");
    }

    @Test
    void browserAcceptLanguageSelectsInitialLocale() throws Exception {
        String html = mockMvc.perform(get("/").header("Accept-Language", "pl-PL,pl;q=0.9,en;q=0.8"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("lang=\"pl\"");
    }

    @Test
    void unsupportedBrowserLocaleFallsBackToEnglish() throws Exception {
        String html = mockMvc.perform(get("/").header("Accept-Language", "fr-FR,fr;q=0.9"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("lang=\"en\"");
    }

    @Test
    void localeCookieOverridesBrowserPreference() throws Exception {
        String html = mockMvc.perform(get("/").cookie(new Cookie("locale", "pl"))
                        .header("Accept-Language", "en-US,en;q=0.9"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("lang=\"pl\"");
    }

    @Test
    void localeCookieSwitchCoversAdminPanelToo() throws Exception {
        String html = mockMvc.perform(get("/admin").cookie(new Cookie("locale", "el")).with(webAdmin("admin-sub")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("lang=\"el\"");
    }

    @Test
    void adminPagesDeployCarryNoUnresolvedKeys() throws Exception {
        for (String path : new String[] {"/admin", "/admin/reservations", "/admin/pricing",
                "/admin/map", "/admin/recurring-reservations", "/admin/content"}) {
            String html = mockMvc.perform(get(path).with(webAdmin("admin-sub")))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).as("page " + path).doesNotContain("??");
        }
    }

    @Test
    void customerPagesDeployCarryNoUnresolvedKeys() throws Exception {
        for (String path : new String[] {"/", "/about", "/contact", "/logged-out"}) {
            String html = mockMvc.perform(get(path))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).as("page " + path).doesNotContain("??");
        }
    }

    @Test
    void reservePageMapShipsJavaScriptStateLabelsForResolvedLocale() throws Exception {
        // The map fragment exposes the localized state text to reserve.js as
        // data attributes; the JS contract says "same strings as the static
        // render", so assert agreement with the MessageSource instead of
        // concrete translations.
        String html = mockMvc.perform(get("/reserve").with(webUser("i18n-sub"))
                        .locale(Locale.forLanguageTag("pl")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        assertThat(dataAttribute(html, "data-text-free"))
                .isEqualTo(messageSource.getMessage("map.free", null, Locale.forLanguageTag("pl")));
        assertThat(dataAttribute(html, "data-text-reserved"))
                .isEqualTo(messageSource.getMessage("map.reserved", null, Locale.forLanguageTag("pl")));
        assertThat(dataAttribute(html, "data-text-selected"))
                .isEqualTo(messageSource.getMessage("map.selected", null, Locale.forLanguageTag("pl")));
    }

    @Test
    void messageBundlesCarryIdenticalKeySets() throws Exception {
        Properties reference = bundle("messages_en.properties");
        for (String name : new String[] {"messages.properties", "messages_pl.properties", "messages_el.properties"}) {
            assertThat(bundle(name).stringPropertyNames()).as("bundle " + name)
                    .isEqualTo(reference.stringPropertyNames());
        }
    }

    @Test
    void messageSourceResolvesSupportedLocalesAndFallsBackToEnglish() {
        Locale polish = Locale.forLanguageTag("pl");
        Locale greek = Locale.forLanguageTag("el");
        assertThat(messageSource.getMessage("reserve.title", null, polish)).isNotBlank();
        assertThat(messageSource.getMessage("reserve.title", null, greek))
                .isNotEqualTo(messageSource.getMessage("reserve.title", null, polish));
        // Locale without a bundle: base messages.properties (= English copy).
        assertThat(messageSource.getMessage("reserve.title", null, Locale.forLanguageTag("fr")))
                .isEqualTo(messageSource.getMessage("reserve.title", null, Locale.ENGLISH));
    }

    private static String dataAttribute(String html, String attribute) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile(attribute + "=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).as("attribute " + attribute).isTrue();
        return matcher.group(1);
    }

    private static Properties bundle(String name) {
        Properties properties = new Properties();
        try (InputStream in = I18nIntegrationTest.class.getClassLoader().getResourceAsStream("messages/" + name)) {
            assertThat(in).as("bundle " + name).isNotNull();
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }
}

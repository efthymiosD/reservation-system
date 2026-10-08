package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.JwtSupport.admin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import com.decoupledx.reservation.webui.EnabledLocales;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Per-locale site-content contract (value-free):
 * <ul>
 *   <li>copy authored for one language ({@code pl:key}) shows only to that
 *       language — per-locale independence,</li>
 *   <li>copy authored on the master key shows only to the default language,</li>
 *   <li>the language toggle ({@code ui.locales.enabled}) hides a language from
 *       the selector and resolves stale cookies / ?lang values to the next
 *       enabled language,</li>
 *   <li>the default language can never be disabled,</li>
 *   <li>the per-language editor emits the selected language's storage keys.</li>
 * </ul>
 * Translations themselves are never asserted — the bundles are presentation,
 * not contract.
 */
@AutoConfigureMockMvc
class SiteContentLocalizationIntegrationTest extends PostgresIntegrationTest {

    private static final String MASTER_KEY = "home.feature.1.title";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetLanguageToggles() {
        jdbc.update("DELETE FROM site_content WHERE key = 'ui.locales.enabled'");
        jdbc.update("DELETE FROM site_content WHERE key IN (?, ?)", MASTER_KEY, "pl:" + MASTER_KEY);
    }

    @Test
    void authoredPerLocaleCopyShowsOnlyToThatLanguage() throws Exception {
        update(MASTER_KEY, "Master English text marker");
        update("pl:" + MASTER_KEY, "Polski tekst znacznik");

        String polish = home(new Cookie("locale", "pl"));
        assertThat(polish).contains("Polski tekst znacznik").doesNotContain("Master English text marker");

        String english = home(new Cookie("locale", "en"));
        assertThat(english).contains("Master English text marker").doesNotContain("Polski tekst znacznik");
    }

    @Test
    void aLanguageWithoutAuthoredCopyFallsBackToTheBundleDefault() throws Exception {
        update(MASTER_KEY, "Master English text marker");
        // Greek has no authored copy and must not leak the English master.
        assertThat(home(new Cookie("locale", "el")))
                .doesNotContain("Master English text marker")
                .doesNotContain("??");
    }

    @Test
    void disablingALanguageHidesItFromSelectorAndResolvesToTheNextEnabledLanguage() throws Exception {
        update(EnabledLocales.ENABLED_LOCALES_KEY, "en,el");
        MvcResult result = homeResult(new Cookie("locale", "pl"));
        assertThat(result.getResponse().getContentAsString()).contains("lang=\"el\"");
        assertThat(navSelectorLanguages(result.getResponse().getContentAsString()))
                .containsExactlyInAnyOrder("en", "el");
    }

    @Test
    void langParamForADisabledLanguageIsSanitized() throws Exception {
        update(EnabledLocales.ENABLED_LOCALES_KEY, "en,pl");
        String html = mockMvc.perform(get("/?lang=el")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("lang=\"en\"");
    }

    @Test
    void cookieForADisabledLanguageFallsBackWithoutBeingRewritten() throws Exception {
        update(EnabledLocales.ENABLED_LOCALES_KEY, "en,el");
        MvcResult result = homeResult(new Cookie("locale", "pl"));
        assertThat(result.getResponse().getContentAsString()).contains("lang=\"el\"");
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    void reEnablingALanguageRestoresIt() throws Exception {
        update(EnabledLocales.ENABLED_LOCALES_KEY, "en,el");
        update(EnabledLocales.ENABLED_LOCALES_KEY, "en,pl,el");
        assertThat(navSelectorLanguages(homeResult(new Cookie("locale", "pl")).getResponse().getContentAsString()))
                .containsExactlyInAnyOrder("en", "pl", "el");
    }

    @Test
    void defaultLanguageCanNeverBeDisabled() throws Exception {
        mockMvc.perform(post("/admin/content/languages")
                        .with(webAdmin("admin-per-language"))
                        .with(csrf())
                        .param("enabled", "pl", "el"))
                .andExpect(status().is3xxRedirection());
        // The default language is never stored disabled: consumers re-add it.
        String stored = jdbc.queryForObject(
                "SELECT body FROM site_content WHERE key = 'ui.locales.enabled'", String.class);
        assertThat(stored).isEqualTo("pl,el");
        String nav = mockMvc.perform(get("/"))
                .andReturn().getResponse().getContentAsString();
        assertThat(navSelectorLanguages(nav)).contains("en");
    }

    @Test
    void perLanguageEditorRendersTheSelectedLanguageStorageKeys() throws Exception {
        update("pl:" + MASTER_KEY, "Polski tekst znacznik");

        String polishEditor = mockMvc.perform(get("/admin/content").param("contentLang", "pl")
                        .with(webAdmin("admin-editor")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        assertThat(polishEditor).contains("pl:" + MASTER_KEY).contains("Polski tekst znacznik");

        String englishEditor = mockMvc.perform(get("/admin/content").param("contentLang", "en")
                        .with(webAdmin("admin-editor")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        assertThat(englishEditor).contains('"' + MASTER_KEY + '"').doesNotContain("pl:" + MASTER_KEY);
    }

    private List<String> navSelectorLanguages(String html) {
        List<String> tags = new ArrayList<>();
        Matcher matcher = Pattern.compile("hreflang=\"([a-z]+)\"").matcher(html);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return tags;
    }

    private String home(Cookie locale) throws Exception {
        return homeResult(locale).getResponse().getContentAsString();
    }

    private MvcResult homeResult(Cookie locale) throws Exception {
        return mockMvc.perform(get("/").cookie(locale)).andReturn();
    }

    private void update(String key, String body) throws Exception {
        mockMvc.perform(put("/api/admin/content/" + key)
                        .with(admin("content-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + body + "\"}"))
                .andExpect(status().isNoContent());
    }
}

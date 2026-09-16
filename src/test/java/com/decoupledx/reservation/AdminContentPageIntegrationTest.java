package com.decoupledx.reservation;

import static com.decoupledx.reservation.testinfra.WebUserSupport.webAdmin;
import static com.decoupledx.reservation.testinfra.WebUserSupport.webUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

/**
 * Core-flow admin site-content page tests (testing convention): role gating,
 * venue profile save, text block save, hero/about photo upload and opening
 * hours save, plus DB/file checks. Field management moved to the dedicated
 * /admin/map editor (AdminMapPageIntegrationTest). Page internals are not
 * asserted.
 */
@AutoConfigureMockMvc
class AdminContentPageIntegrationTest extends PostgresIntegrationTest {

    private static final Path UPLOAD_DIR = Path.of("target/content-test-uploads");

    @DynamicPropertySource
    static void uploadDirProperty(DynamicPropertyRegistry registry) {
        registry.add("app.content.upload-dir", UPLOAD_DIR::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetUploadDir() throws Exception {
        Files.createDirectories(UPLOAD_DIR);
        try (var stream = Files.list(UPLOAD_DIR)) {
            stream.filter(Files::isRegularFile).forEach(f -> f.toFile().delete());
        }
    }

    @Test
    void contentPageRedirectsAnonymousVisitorsToLogin() throws Exception {
        mockMvc.perform(get("/admin/content")).andExpect(status().is3xxRedirection());
    }

    @Test
    void contentPageIsForbiddenForCustomers() throws Exception {
        mockMvc.perform(get("/admin/content").with(webUser("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void contentPageLoadsForAdmins() throws Exception {
        mockMvc.perform(get("/admin/content").with(webAdmin("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUpdateVenueProfileFromThePage() throws Exception {
        mockMvc.perform(post("/admin/content/venue")
                        .with(webAdmin("admin"))
                        .with(csrf())
                        .param("name", "Five-a-Side Football Centre")
                        .param("description", "Six floodlit 5x5 fields.")
                        .param("address", "Updated Address 99, 00-000 Wrocław"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/content"));

        assertThat(jdbc.queryForObject(
                "SELECT address FROM venues WHERE id = 'a0000000-0000-0000-0000-000000000001'",
                String.class)).isEqualTo("Updated Address 99, 00-000 Wrocław");
    }

    @Test
    void adminCanUpdateSiteContentFromThePage() throws Exception {
        mockMvc.perform(post("/admin/content/contact.contact.body")
                        .with(webAdmin("admin"))
                        .with(csrf())
                        .param("body", "Custom contact text."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/content"));

        assertThat(jdbc.queryForObject(
                "SELECT body FROM site_content WHERE key = 'contact.contact.body'", String.class))
                .isEqualTo("Custom contact text.");
    }

    @Test
    void adminCanUploadHeroPhotoFromThePage() throws Exception {
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "hero.png", "image/png", pngBytes);

        mockMvc.perform(multipart("/admin/content/photo")
                        .file(photo)
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/content"));

        String webPath = jdbc.queryForObject(
                "SELECT body FROM site_content WHERE key = 'home.hero.photo'", String.class);
        assertThat(webPath).startsWith("/uploads/hero-").endsWith(".png");
        String fileName = Path.of(webPath).getFileName().toString();
        assertThat(Files.exists(UPLOAD_DIR.resolve(fileName))).isTrue();
    }

    @Test
    void adminCanUploadAboutPhotoFromThePage() throws Exception {
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "about.png", "image/png", pngBytes);

        mockMvc.perform(multipart("/admin/content/photo")
                        .file(photo)
                        .param("photoKey", "about.photo")
                        .with(webAdmin("admin"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/content"));

        String webPath = jdbc.queryForObject(
                "SELECT body FROM site_content WHERE key = 'about.photo'", String.class);
        assertThat(webPath).startsWith("/uploads/about-").endsWith(".png");
        String fileName = Path.of(webPath).getFileName().toString();
        assertThat(Files.exists(UPLOAD_DIR.resolve(fileName))).isTrue();
    }

    @Test
    void adminCanUpdateOpeningHoursFromThePage() throws Exception {
        MockHttpServletRequestBuilder post = post("/admin/content/opening-hours")
                .with(webAdmin("admin"))
                .with(csrf());
        for (DayOfWeek day : DayOfWeek.values()) {
            post.param("enabled-" + day, "on")
                    .param("opens-" + day, "14:00")
                    .param("closes-" + day, "23:00");
        }

        mockMvc.perform(post)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/content"));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM opening_hours WHERE venue_id = 'a0000000-0000-0000-0000-000000000001'",
                Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject(
                "SELECT opens_at::text FROM opening_hours WHERE venue_id = 'a0000000-0000-0000-0000-000000000001'"
                        + " AND day_of_week = 'MONDAY'", String.class)).startsWith("14:00");
    }
}
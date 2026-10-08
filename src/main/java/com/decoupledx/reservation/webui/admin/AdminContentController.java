package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.venue.adapter.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.adapter.api.OpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.webui.EnabledLocales;
import com.decoupledx.reservation.webui.SupportedLocales;
import com.decoupledx.reservation.webui.WebMessages;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin site-content page (ROLE_ADMIN gated at the security chain): edits the
 * venue profile (name, description, address), replaces the home/about photos,
 * edits the weekly opening hours and every public-page text block. All changes
 * go through the module APIs and are revalidated server-side. The reservation
 * map fields live on the dedicated /admin/map layout editor.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
class AdminContentController {

    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final AdminContentFactory contentFactory;
    private final WebMessages messages;
    private final VenueApi venueApi;
    private final ContentApi contentApi;

    @Value("${app.content.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/admin/content")
    String content(@RequestParam(defaultValue = "en") String contentLang, Model model) {
        model.addAttribute("page", contentFactory.build(contentLang));
        model.addAttribute("contentLang", contentLang.toLowerCase());
        return "admin/content";
    }

    @PostMapping("/admin/content/languages")
    String updateLanguages(@RequestParam(defaultValue = "en") String contentLang,
                           @RequestParam(required = false) List<String> enabled,
                           RedirectAttributes redirect) {
        // The default language is always on — it is the final fallback.
        String csv = SupportedLocales.tags().stream()
                .filter(tag -> !"en".equals(tag))
                .filter(tag -> enabled != null && enabled.contains(tag))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        contentApi.update(EnabledLocales.ENABLED_LOCALES_KEY, csv);
        redirect.addFlashAttribute("message", messages.get("admin.content.languagesSaved"));
        return "redirect:/admin/content?contentLang=" + contentLang;
    }

    @PostMapping("/admin/content/venue")
    String updateVenue(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String address,
            RedirectAttributes redirect) {
        try {
            venueApi.updateProfile(venueApi.singleVenueId(), name.trim(), description, address);
            redirect.addFlashAttribute("message", messages.get("admin.content.venueSaved"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/admin/content/photo")
    String uploadPhoto(@RequestParam("photo") MultipartFile photo,
                       @RequestParam(value = "photoKey", defaultValue = "home.hero.photo") String photoKey,
                       RedirectAttributes redirect) {
        try {
            if (!photoKey.equals(AdminContentFactory.PHOTO_KEY)
                    && !photoKey.equals(AdminContentFactory.ABOUT_PHOTO_KEY)) {
                throw new IllegalArgumentException(messages.get("admin.content.unknownPhotoKey"));
            }
            String prefix = photoKey.equals(AdminContentFactory.ABOUT_PHOTO_KEY) ? "about-" : "hero-";
            String webPath = savePhoto(photo, prefix);
            contentApi.update(photoKey, webPath);
            redirect.addFlashAttribute("message", messages.get(photoKey.equals(AdminContentFactory.ABOUT_PHOTO_KEY)
                    ? "admin.content.aboutPhotoUpdated"
                    : "admin.content.heroPhotoUpdated"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        } catch (IOException exception) {
            log.error("Failed to store site photo", exception);
            redirect.addFlashAttribute("error", messages.get("admin.content.photoStoreFailed"));
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/admin/content/opening-hours")
    String updateOpeningHours(@RequestParam Map<String, String> params, RedirectAttributes redirect) {
        try {
            Map<DayOfWeek, DailyOpeningHours> days = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                if (!params.containsKey("enabled-" + day)) {
                    continue;
                }
                LocalTime opensAt = parseTime(params.get("opens-" + day));
                LocalTime closesAt = parseTime(params.get("closes-" + day));
                if (opensAt == null || closesAt == null || opensAt.equals(closesAt)) {
                    throw new IllegalArgumentException(messages.get("admin.content.differingHoursRequired"));
                }
                days.put(day, new DailyOpeningHours(opensAt, closesAt));
            }
            venueApi.updateOpeningHours(venueApi.singleVenueId(), new OpeningHours(days));
            redirect.addFlashAttribute("message", messages.get("admin.content.hoursSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/admin/content/{key}")
    String updateBlock(@PathVariable String key, @RequestParam String body, RedirectAttributes redirect) {
        try {
            contentApi.update(key, body);
            redirect.addFlashAttribute("message", messages.get("admin.content.textSaved"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/admin/content";
    }

    private String savePhoto(MultipartFile photo, String prefix) throws IOException {
        if (photo.isEmpty()) {
            throw new IllegalArgumentException(messages.get("admin.content.chooseImage"));
        }
        String extension = switch (photo.getContentType() == null ? "" : photo.getContentType()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpeg" -> "jpg";
            default -> throw new IllegalArgumentException(messages.get("admin.content.imageTypeRejected"));
        };
        if (photo.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException(messages.get("admin.content.imageTooLarge"));
        }
        Path targetDir = Path.of(uploadDir);
        Files.createDirectories(targetDir);
        String fileName = prefix + System.currentTimeMillis() + "." + extension;
        try (InputStream in = photo.getInputStream()) {
            Files.copy(in, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + fileName;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private String userMessage(BusinessException exception) {
        return messages.errorMessage(exception);
    }
}
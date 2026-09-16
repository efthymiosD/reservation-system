package com.decoupledx.reservation.webui.admin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.decoupledx.reservation.content.api.ContentApi;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.venue.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.api.OpeningHours;
import com.decoupledx.reservation.venue.api.VenueApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final VenueApi venueApi;
    private final ContentApi contentApi;

    @Value("${app.content.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/admin/content")
    String content(Model model) {
        model.addAttribute("page", contentFactory.build());
        return "admin/content";
    }

    @PostMapping("/admin/content/venue")
    String updateVenue(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String address,
            RedirectAttributes redirect) {
        try {
            venueApi.updateProfile(venueApi.singleVenueId(), name.trim(), description, address);
            redirect.addFlashAttribute("message", "Venue profile saved.");
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
                throw new IllegalArgumentException("Unknown photo key.");
            }
            String prefix = photoKey.equals(AdminContentFactory.ABOUT_PHOTO_KEY) ? "about-" : "hero-";
            String webPath = savePhoto(photo, prefix);
            contentApi.update(photoKey, webPath);
            redirect.addFlashAttribute("message", photoKey.equals(AdminContentFactory.ABOUT_PHOTO_KEY)
                    ? "About page photo updated."
                    : "Hero photo updated.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        } catch (IOException exception) {
            log.error("Failed to store site photo", exception);
            redirect.addFlashAttribute("error", "The image could not be stored. Please try again.");
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
                    throw new IllegalArgumentException(
                            "Each opened day needs an opening time and a closing time that differ.");
                }
                days.put(day, new DailyOpeningHours(opensAt, closesAt));
            }
            venueApi.updateOpeningHours(venueApi.singleVenueId(), new OpeningHours(days));
            redirect.addFlashAttribute("message", "Opening hours saved.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/content";
    }

    @PostMapping("/admin/content/{key}")
    String updateBlock(@PathVariable String key, @RequestParam String body, RedirectAttributes redirect) {
        try {
            contentApi.update(key, body);
            redirect.addFlashAttribute("message", "Text saved.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", userMessage(exception));
        }
        return "redirect:/admin/content";
    }

    private String savePhoto(MultipartFile photo, String prefix) throws IOException {
        if (photo.isEmpty()) {
            throw new IllegalArgumentException("Choose an image to upload.");
        }
        String extension = switch (photo.getContentType() == null ? "" : photo.getContentType()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpeg" -> "jpg";
            default -> throw new IllegalArgumentException("Only PNG, JPEG or WebP images are accepted.");
        };
        if (photo.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("The image must be 5 MB or smaller.");
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
        return switch (exception.errorCode()) {
            case INVALID_VENUE_NAME -> "The venue name must not be empty.";
            default -> exception.getMessage();
        };
    }
}
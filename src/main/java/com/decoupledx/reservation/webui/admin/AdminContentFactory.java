package com.decoupledx.reservation.webui.admin;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.content.api.ContentApi;
import com.decoupledx.reservation.venue.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.api.VenueApi;
import com.decoupledx.reservation.venue.api.VenueInfo;
import com.decoupledx.reservation.webui.reserve.VenueLayoutLoader;

import lombok.RequiredArgsConstructor;

/**
 * Assembles the site-content admin page: the editable venue profile, the hero
 * and about photos, the weekly opening hours and every text block. Block labels
 * are a presentation concern and live here, not in the content module. The
 * reservation map fields are edited on the dedicated /admin/map page.
 */
@Component
@RequiredArgsConstructor
class AdminContentFactory {

    static final String PHOTO_KEY = "home.hero.photo";
    static final String ABOUT_PHOTO_KEY = "about.photo";

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> NON_TEXT_KEYS =
            Set.of(PHOTO_KEY, ABOUT_PHOTO_KEY, VenueLayoutLoader.LAYOUT_KEY);

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("home.feature.1.title", "Home — feature 1 title"),
            Map.entry("home.feature.1.body", "Home — feature 1 text"),
            Map.entry("home.feature.2.title", "Home — feature 2 title"),
            Map.entry("home.feature.2.body", "Home — feature 2 text"),
            Map.entry("home.feature.3.title", "Home — feature 3 title"),
            Map.entry("home.feature.3.body", "Home — feature 3 text"),
            Map.entry("about.booking.body", "About — booking text"),
            Map.entry("contact.getting_here.body", "Contact — getting here"),
            Map.entry("contact.contact.body", "Contact — contact text"),
            Map.entry("contact.phone", "Contact — phone"),
            Map.entry("contact.email", "Contact — email"));

    private final VenueApi venueApi;
    private final ContentApi contentApi;

    AdminContentModel build() {
        VenueInfo venue = venueApi.getVenue(venueApi.singleVenueId());
        List<AdminContentModel.ContentBlockView> blocks = contentApi.all().stream()
                .filter(block -> !NON_TEXT_KEYS.contains(block.key()))
                .map(block -> new AdminContentModel.ContentBlockView(
                        block.key(), LABELS.getOrDefault(block.key(), block.key()), block.body()))
                .toList();

        return new AdminContentModel(
                venue,
                blocks,
                contentApi.get(PHOTO_KEY),
                contentApi.get(ABOUT_PHOTO_KEY),
                weeklyHours(venue),
                venue.id().value());
    }

    private List<AdminContentModel.DayHoursView> weeklyHours(VenueInfo venue) {
        List<AdminContentModel.DayHoursView> weekly = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            Optional<DailyOpeningHours> hours = venue.openingHours().on(day);
            weekly.add(new AdminContentModel.DayHoursView(
                    day.name(),
                    day.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    hours.isPresent(),
                    hours.map(hoursOfDay -> hoursOfDay.opensAt().format(HH_MM)).orElse(""),
                    hours.map(hoursOfDay -> hoursOfDay.closesAt().format(HH_MM)).orElse("")));
        }
        return weekly;
    }
}
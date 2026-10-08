package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import com.decoupledx.reservation.venue.adapter.api.DailyOpeningHours;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import com.decoupledx.reservation.webui.EnabledLocales;
import com.decoupledx.reservation.webui.SupportedLocales;
import com.decoupledx.reservation.webui.reserve.VenueLayoutLoader;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Assembles the site-content admin page: the editable venue profile, the hero
 * and about photos, the weekly opening hours and the per-language text blocks
 * (the language picked by the {@code ?contentLang} dropdown). Block labels are
 * a presentation concern and live here, not in the content module. The
 * reservation map fields are edited on the dedicated /admin/map page.
 */
@Component
@RequiredArgsConstructor
class AdminContentFactory {

    static final String PHOTO_KEY = "home.hero.photo";
    static final String ABOUT_PHOTO_KEY = "about.photo";

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> NON_TEXT_KEYS =
            Set.of(PHOTO_KEY, ABOUT_PHOTO_KEY, VenueLayoutLoader.LAYOUT_KEY, EnabledLocales.ENABLED_LOCALES_KEY);

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
    private final EnabledLocales enabledLocales;
    private final MessageSource messageSource;

    AdminContentModel build(String contentLanguage) {
        String language = contentLanguage != null ? contentLanguage.toLowerCase() : "en";
        VenueInfo venue = venueApi.getVenue(venueApi.singleVenueId());

        List<LanguageOptionView> languages = SupportedLocales.ALL.stream()
                .map(locale -> new LanguageOptionView(
                        locale.getLanguage(),
                        locale.getDisplayLanguage(LocaleContextHolder.getLocale()),
                        language.equals(locale.getLanguage())))
                .toList();

        List<DayHoursView> weekly = weeklyHours(venue);

        return new AdminContentModel(
                venue,
                textBlocks(language),
                languages,
                weekly,
                enabledSwitches(),
                contentApi.get(PHOTO_KEY),
                contentApi.get(ABOUT_PHOTO_KEY),
                venue.id().value());
    }

    /**
     * Text blocks for one editor language. The storage key is the master key
     * for the default language, locale-prefixed for all others; the built-in
     * bundle default is the input placeholder. Authored copy shows verbatim;
     * an empty textarea means "use the built-in default".
     */
    private List<ContentBlockView> textBlocks(String language) {
        String prefix = SupportedLocales.DEFAULT.getLanguage().equals(language) ? "" : language + ":";
        Map<String, String> authored = contentApi.all().stream()
                .collect(Collectors.toMap(com.decoupledx.reservation.content.adapter.api.SiteContentBlock::key,
                        com.decoupledx.reservation.content.adapter.api.SiteContentBlock::body,
                        (first, second) -> first));
        List<String> textKeys = List.of(
                "home.feature.1.title", "home.feature.1.body",
                "home.feature.2.title", "home.feature.2.body",
                "home.feature.3.title", "home.feature.3.body",
                "about.booking.body",
                "contact.getting_here.body", "contact.contact.body");
        return textKeys.stream()
                .map(masterKey -> new ContentBlockView(
                        masterKey,
                        prefix + masterKey,
                        LABELS.get(masterKey),
                        authored.getOrDefault(prefix + masterKey, ""),
                        bundleDefault(masterKey)))
                .toList();
    }

    private String bundleDefault(String masterKey) {
        String bundleKey = LABELS.get(masterKey) == null ? masterKey : bundleKeyFor(masterKey);
        return messageSource.getMessage(bundleKey, null, "", LocaleContextHolder.getLocale());
    }

    /** Content-block master key → bundle default key. */
    private static String bundleKeyFor(String masterKey) {
        return switch (masterKey) {
            case "home.feature.1.title" -> "home.feature1.title";
            case "home.feature.1.body" -> "home.feature1.body";
            case "home.feature.2.title" -> "home.feature2.title";
            case "home.feature.2.body" -> "home.feature2.body";
            case "home.feature.3.title" -> "home.feature3.title";
            case "home.feature.3.body" -> "home.feature3.body";
            case "about.booking.body" -> "about.bookingBody";
            case "contact.getting_here.body" -> "contact.gettingHereBody";
            case "contact.contact.body" -> "contact.contactBody";
            default -> masterKey;
        };
    }

    private Map<String, Boolean> enabledSwitches() {
        List<String> active = enabledLocales.active().stream().map(Locale::getLanguage).toList();
        return SupportedLocales.tags().stream()
                .collect(Collectors.toMap(Function.identity(), active::contains));
    }

    private List<DayHoursView> weeklyHours(VenueInfo venue) {
        List<DayHoursView> weekly = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            Optional<DailyOpeningHours> hours = venue.openingHours().on(day);
            weekly.add(new DayHoursView(
                    day.name(),
                    day.getDisplayName(TextStyle.FULL, LocaleContextHolder.getLocale()),
                    hours.isPresent(),
                    hours.map(hoursOfDay -> hoursOfDay.opensAt().format(HH_MM)).orElse(""),
                    hours.map(hoursOfDay -> hoursOfDay.closesAt().format(HH_MM)).orElse("")));
        }
        return weekly;
    }
}

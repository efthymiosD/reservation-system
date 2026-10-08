package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * View model for the /admin/content page. Text blocks are emitted for one
 * editor language at a time (the {@code ?contentLang} dropdown); every block
 * carries its storage key for that language and the built-in default as
 * placeholder. The languages card holds one switch per supported language.
 */
record AdminContentModel(
        VenueInfo venue,
        List<ContentBlockView> blocks,
        List<LanguageOptionView> languages,
        List<DayHoursView> weekly,
        Map<String, Boolean> enabledLanguages,
        String photoPath,
        String aboutPhotoPath,
        UUID venueId) {

}

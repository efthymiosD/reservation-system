package com.decoupledx.reservation.webui.admin;

import java.util.List;
import java.util.UUID;

import com.decoupledx.reservation.venue.adapter.api.VenueInfo;

/** View model for the /admin/content page. */
public record AdminContentModel(
        VenueInfo venue,
        List<ContentBlockView> blocks,
        String photoPath,
        String aboutPhotoPath,
        List<DayHoursView> weekly,
        UUID venueId) {

    public record ContentBlockView(String key, String label, String body) {
    }

    public record DayHoursView(String dayOfWeek, String label, boolean enabled, String opensAt, String closesAt) {
    }
}
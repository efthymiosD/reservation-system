package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.venue.adapter.api.VenueInfo;
import java.util.List;
import java.util.UUID;

/**
 * View model for the /admin/content page.
 */
record AdminContentModel(
        VenueInfo venue,
        List<ContentBlockView> blocks,
        String photoPath,
        String aboutPhotoPath,
        List<DayHoursView> weekly,
        UUID venueId) {

}

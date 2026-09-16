package com.decoupledx.reservation.webui.reserve;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.decoupledx.reservation.content.api.ContentApi;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads the venue layout for the reservation page venue map. The layout is
 * admin-editable and persists in the site-content block {@code venue.map.layout}
 * (seeded to mirror the bundled {@code webui/venue-layout.json}). It is parsed
 * on every read so admin changes take effect without a restart; an empty or
 * unparseable block falls back to the seeded classpath file.
 */
@Component
@RequiredArgsConstructor
public class VenueLayoutLoader {

    public static final String LAYOUT_KEY = "venue.map.layout";

    private final ContentApi contentApi;
    private final ObjectMapper objectMapper;

    public VenueLayout get() {
        String stored = contentApi.get(LAYOUT_KEY);
        if (!stored.isBlank()) {
            try {
                return objectMapper.readValue(stored, VenueLayout.class);
            } catch (Exception exception) {
                // fall through to the seeded classpath layout
            }
        }
        return readClasspath();
    }

    private VenueLayout readClasspath() {
        try (InputStream in = new ClassPathResource("webui/venue-layout.json").getInputStream()) {
            return objectMapper.readValue(in, VenueLayout.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot load webui/venue-layout.json", exception);
        }
    }
}
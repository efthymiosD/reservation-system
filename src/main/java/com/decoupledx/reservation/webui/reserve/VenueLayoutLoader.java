package com.decoupledx.reservation.webui.reserve;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * Loads the seeded venue layout from the classpath once at startup.
 */
@Component
class VenueLayoutLoader {

    private final VenueLayout layout;

    VenueLayoutLoader(ObjectMapper objectMapper) {
        this.layout = read(objectMapper);
    }

    VenueLayout get() {
        return layout;
    }

    private static VenueLayout read(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("webui/venue-layout.json").getInputStream()) {
            return objectMapper.readValue(in, VenueLayout.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot load webui/venue-layout.json", exception);
        }
    }
}

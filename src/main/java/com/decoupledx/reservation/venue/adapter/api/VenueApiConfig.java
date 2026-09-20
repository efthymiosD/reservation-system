package com.decoupledx.reservation.venue.adapter.api;

import com.decoupledx.reservation.venue.domain.port.VenueService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VenueApiConfig {

    @Bean
    VenueApi venueApi(VenueService venueService) {
        return new VenueApiImpl(venueService);
    }
}

package com.decoupledx.reservation.venue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.venue.domain.port.VenueRepository;
import com.decoupledx.reservation.venue.domain.service.VenueService;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class VenueModuleConfig {

    @Bean
    VenueService venueService(VenueRepository venueRepository, TransactionRunner tx) {
        return new VenueService(venueRepository, tx);
    }
}

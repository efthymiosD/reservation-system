package com.decoupledx.reservation.venue.domain;

import com.decoupledx.reservation.shared.TransactionRunner;
import com.decoupledx.reservation.venue.domain.port.VenueRepository;
import com.decoupledx.reservation.venue.domain.port.VenueService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class VenueDomainConfig {

    @Bean
    VenueService venueService(VenueRepository venueRepository, TransactionRunner tx) {
        return new VenueServiceImpl(venueRepository, tx);
    }
}

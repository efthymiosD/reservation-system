package com.decoupledx.reservation.content.domain;

import com.decoupledx.reservation.content.domain.port.ContentService;
import com.decoupledx.reservation.content.domain.port.SiteContentRepository;
import com.decoupledx.reservation.shared.TransactionRunner;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class SiteContentDomainConfig {

    @Bean
    ContentService contentService(SiteContentRepository siteContentRepository,
                                  TransactionRunner tx, Clock clock) {
        return new SiteContentServiceImpl(siteContentRepository, tx, clock);
    }
}

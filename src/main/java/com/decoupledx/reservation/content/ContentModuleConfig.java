package com.decoupledx.reservation.content;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.content.domain.port.SiteContentRepository;
import com.decoupledx.reservation.content.domain.service.SiteContentService;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class ContentModuleConfig {

    @Bean
    SiteContentService siteContentService(SiteContentRepository siteContentRepository,
                                          TransactionRunner tx, Clock clock) {
        return new SiteContentService(siteContentRepository, tx, clock);
    }
}
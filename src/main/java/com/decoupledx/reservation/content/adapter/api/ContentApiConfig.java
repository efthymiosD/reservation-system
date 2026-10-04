package com.decoupledx.reservation.content.adapter.api;

import com.decoupledx.reservation.content.domain.port.ContentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ContentApiConfig {

    @Bean
    ContentApi contentApi(ContentService contentService) {
        return new ContentApiImpl(contentService);
    }
}

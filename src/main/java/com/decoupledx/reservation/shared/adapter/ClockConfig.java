package com.decoupledx.reservation.shared.adapter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.clock.fixed-instant:}") String fixedInstant) {
        if (fixedInstant == null || fixedInstant.isBlank()) {
            return Clock.systemUTC();
        }
        return Clock.fixed(Instant.parse(fixedInstant), ZoneOffset.UTC);
    }
}

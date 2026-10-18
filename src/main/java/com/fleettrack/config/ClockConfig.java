package com.fleettrack.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Injecting a Clock instead of calling Instant.now() everywhere makes time-dependent
 * logic (ETAs, timestamps, stale-ping detection) deterministic in tests.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

package com.aicompass.compass.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean
    Clock compassClock() {
        return Clock.systemUTC();
    }
}

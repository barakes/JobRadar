package com.jobradar.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on scheduled tasks. Can be switched off with jobradar.sync.enabled=false (useful in tests).
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "jobradar.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
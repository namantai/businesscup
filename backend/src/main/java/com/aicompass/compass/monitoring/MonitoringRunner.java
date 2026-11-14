package com.aicompass.compass.monitoring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "compass.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringRunner {
    private final MonitoringService monitoring;
    public MonitoringRunner(MonitoringService monitoring) { this.monitoring=monitoring; }
    @Scheduled(fixedDelayString = "${compass.monitoring.delay-ms:60000}", initialDelay = 60000)
    public void tick() {
        monitoring.enqueue();
        // One bounded recomputation per minute per instance. PostgreSQL claims coordinate instances.
        var job=monitoring.claim();
        if(job==null) return;
        try { monitoring.process(job); }
        catch(RuntimeException failure) { monitoring.retry(job); }
    }
}

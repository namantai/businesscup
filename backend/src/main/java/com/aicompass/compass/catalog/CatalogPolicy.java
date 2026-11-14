package com.aicompass.compass.catalog;

import java.util.Arrays;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@org.springframework.core.annotation.Order(0)
public class CatalogPolicy implements ApplicationRunner {
    private final boolean allowDemo;
    private final JdbcTemplate jdbc;

    public CatalogPolicy(Environment environment, JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        allowDemo = environment.getProperty("compass.catalog.allow-demo", Boolean.class, false);
        String[] profiles = environment.getActiveProfiles();
        if (allowDemo && (profiles.length == 0 || !Arrays.stream(profiles).allMatch(Set.of("dev", "test")::contains))) {
            throw new IllegalStateException("Demo catalog is permitted only with explicit dev/test profiles");
        }
    }

    public boolean allowDemo() { return allowDemo; }

    @Override
    public void run(ApplicationArguments args) {
        if (!allowDemo && jdbc.queryForObject("SELECT count(*) FROM compass.catalog_version WHERE dataset_kind = 'DEMO'", Long.class) > 0) {
            throw new IllegalStateException("Demo catalog exists in a database with demo disabled");
        }
    }
}

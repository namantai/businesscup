package com.aicompass.compass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("compass.integrations")
public record IntegrationSettings(String llmMode, String paymentsMode, String emailMode,
        boolean sourceFetchEnabled) {
    public IntegrationSettings {
        if (!"OFF".equals(llmMode) || !"OFF".equals(paymentsMode)
                || !"LOCAL".equals(emailMode) || sourceFetchEnabled) {
            throw new IllegalArgumentException("P01 requires external integrations OFF and email LOCAL");
        }
    }
}

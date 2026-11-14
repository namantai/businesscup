package com.aicompass.compass.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegrationSettingsTest {
    @Test
    void acceptsOfflineConfiguration() {
        assertDoesNotThrow(() -> new IntegrationSettings("OFF", "OFF", "LOCAL", false));
    }

    @Test
    void rejectsLlmCalls() {
        assertThrows(IllegalArgumentException.class, () -> new IntegrationSettings("LIVE", "OFF", "LOCAL", false));
    }

    @Test
    void rejectsPayments() {
        assertThrows(IllegalArgumentException.class, () -> new IntegrationSettings("OFF", "LIVE", "LOCAL", false));
    }

    @Test
    void rejectsExternalEmail() {
        assertThrows(IllegalArgumentException.class, () -> new IntegrationSettings("OFF", "OFF", "SMTP", false));
    }

    @Test
    void rejectsSourceFetching() {
        assertThrows(IllegalArgumentException.class, () -> new IntegrationSettings("OFF", "OFF", "LOCAL", true));
    }
}

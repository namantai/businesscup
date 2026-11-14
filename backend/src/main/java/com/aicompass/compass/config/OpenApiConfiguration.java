package com.aicompass.compass.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean OpenAPI catalogOpenApi() {
        return new OpenAPI().info(new Info().title("AI Compass public API").version("1.0.0")
                .description("Published catalog and deterministic read-only recommendation preview. Money is a decimal string; null is unknown. Use version to pin reads."))
                .servers(List.of(new Server().url("/")));
    }

    @Bean OpenApiCustomizer explicitResponseProperties() {
        // These record DTOs always serialize every property, including explicit nulls.
        return api -> api.getComponents().getSchemas().values().forEach(schema -> {
            if (schema.getProperties() != null) schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        });
    }
}

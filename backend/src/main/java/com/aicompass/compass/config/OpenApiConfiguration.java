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
                .description("Published catalog, deterministic guest preview and session-protected personal data. Money is a decimal string; null is unknown. Use version to pin catalog reads. Mutations require a current session-bound CSRF token from /api/v1/auth/csrf."))
                .servers(List.of(new Server().url("/")));
    }

    @Bean OpenApiCustomizer explicitResponseProperties() {
        // These record DTOs always serialize every property, including explicit nulls.
        return api -> api.getComponents().getSchemas().values().forEach(schema -> {
            if (schema.getProperties() != null) schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        });
    }

    @Bean OpenApiCustomizer personalApiSecurityAndErrors() {
        return api -> {
            // OpenAPI 3.0 ignores nullable siblings of $ref. Wrap the referenced account explicitly.
            var guestAccount = new io.swagger.v3.oas.models.media.Schema<>().type("object").nullable(true);
            guestAccount.addAllOfItem(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/Account"));
            api.getComponents().getSchemas().get("SessionStatus").getProperties().put("account", guestAccount);
            api.getComponents().addSecuritySchemes("sessionCookie", new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                    .in(io.swagger.v3.oas.models.security.SecurityScheme.In.COOKIE).name("SESSION"));
            api.getPaths().forEach((path, item) -> {
                boolean personal = path.startsWith("/api/v1/monitoring/") || path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/subscriptions") || path.startsWith("/api/v1/me") || path.startsWith("/api/v1/profiles")
                        || path.startsWith("/api/v1/saved-stacks")
                        || path.startsWith("/api/v1/recommendations") && !path.endsWith("/preview");
                if (!personal && !path.startsWith("/api/v1/auth/")) return;
                item.readOperations().forEach(operation -> {
                    if (personal) operation.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("sessionCookie"));
                    for (int status : List.of(400, 401, 403, 404, 409, 413, 429, 500, 503)) {
                        operation.getResponses().addApiResponse(Integer.toString(status), new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description(org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase())
                                .content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/problem+json",
                                        new io.swagger.v3.oas.models.media.MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ApiProblem")))));
                    }
                });
            });
        };
    }
}

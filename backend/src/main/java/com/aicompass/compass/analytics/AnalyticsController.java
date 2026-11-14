package com.aicompass.compass.analytics;

import com.aicompass.compass.identity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.AssertTrue;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/v1/me/analytics",produces="application/json")
public class AnalyticsController {
    private final UsageAnalyticsService service;
    private final IdentityJson json;
    private final AuthRateLimiter rates;
    public AnalyticsController(UsageAnalyticsService service,IdentityJson json,AuthRateLimiter rates) {this.service=service;this.json=json;this.rates=rates;}
    public record AnalyticsConsent(boolean enabled,@AssertTrue boolean confirm) {}
    @GetMapping public UsageAnalyticsService.AnalyticsPreferences analyticsPreferences(Principal p) {return service.preferences(UUID.fromString(p.getName()));}
    @PatchMapping(consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=AnalyticsConsent.class))))
    public UsageAnalyticsService.AnalyticsPreferences analyticsConsent(Principal p,@RequestBody String body) {
        rates.take("analytics:"+p.getName(),30);var in=json.read(body,AnalyticsConsent.class);return service.configure(UUID.fromString(p.getName()),in.enabled());
    }
    public record AnalyticsVisit(@AssertTrue boolean confirm) {}
    @PostMapping(value="/revisit",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=AnalyticsVisit.class))))
    public void analyticsRevisit(Principal p,@RequestBody String body) {
        rates.take("analytics:"+p.getName(),30);json.read(body,AnalyticsVisit.class);
        service.record(UUID.fromString(p.getName()),UsageAnalyticsService.Event.REVISIT);
    }
}

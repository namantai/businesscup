package com.aicompass.compass.monitoring;

import com.aicompass.compass.identity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/monitoring", produces = "application/json")
public class MonitoringController {
    private final MonitoringService service;
    private final IdentityJson json;
    private final AuthRateLimiter rates;
    public MonitoringController(MonitoringService service, IdentityJson json, AuthRateLimiter rates) {
        this.service=service; this.json=json; this.rates=rates;
    }
    public record WatchWrite(@NotNull UUID stackId, boolean emailOptIn, @AssertTrue boolean consent) {}
    public record Preferences(boolean active, boolean emailOptIn, @AssertTrue boolean confirm) {}
    public record Acknowledgement(boolean appliedSelfReported) {}
    private UUID owner(Principal p) { return UUID.fromString(p.getName()); }
    @GetMapping("/watches") public List<MonitoringService.Watch> watches(Principal p) { return service.watches(owner(p)); }
    @GetMapping("/notices") public List<MonitoringService.Notice> notices(Principal p) { return service.notices(owner(p)); }
    @PostMapping(value = "/watches", consumes = "application/json")
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = WatchWrite.class))))
    public MonitoringService.Watch watch(Principal p, @RequestBody String body) {
        rates.take("watch:"+p.getName(),20); var in=json.read(body,WatchWrite.class);
        return service.watch(owner(p),in.stackId(),in.emailOptIn());
    }
    @PatchMapping(value = "/watches/{id}", consumes = "application/json")
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = Preferences.class))))
    public MonitoringService.Watch preferences(Principal p, @PathVariable UUID id, @RequestBody String body) {
        var in=json.read(body,Preferences.class);return service.preferences(owner(p),id,in.active(),in.emailOptIn(),in.confirm());
    }
    @PatchMapping(value = "/notices/{id}", consumes = "application/json")
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = Acknowledgement.class))))
    public void acknowledge(Principal p, @PathVariable UUID id, @RequestBody String body) {
        service.acknowledge(owner(p),id,json.read(body,Acknowledgement.class).appliedSelfReported());
    }
}

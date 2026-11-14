package com.aicompass.compass.billing;

import com.aicompass.compass.identity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/v1",produces="application/json")
public class EntitlementController {
    private final EntitlementService service;
    private final IdentityJson json;
    private final AuthRateLimiter rates;
    public EntitlementController(EntitlementService service,IdentityJson json,AuthRateLimiter rates) { this.service=service;this.json=json;this.rates=rates; }
    public record BetaPlan(String name,int profileLimit,int watchLimit,boolean history,String feeAmount,String currency,String availability) {}
    public record GrantWrite(@NotNull UUID requestId,@Pattern(regexp="GRANT|REVOKE") @NotNull String action,@Min(0) @Max(90) int days,@Min(0) long expectedRevision) {}
    @GetMapping("/billing/plans") public List<BetaPlan> betaPlans() {
        return List.of(new BetaPlan("FREE",1,0,false,"0.00","USD","Доступен бесплатно"),new BetaPlan("PREMIUM",5,5,true,"0.00","USD","Beta: временный доступ по приглашению, оплата недоступна"));
    }
    @GetMapping("/me/entitlements") public EntitlementService.Entitlements entitlements(Principal p) {return service.current(UUID.fromString(p.getName()));}
    @GetMapping("/me/entitlements/audit") public List<EntitlementService.BetaGrantAudit> entitlementAudit(Principal p) {return service.audit(UUID.fromString(p.getName()));}
    @GetMapping("/admin/entitlements/{owner}") public EntitlementService.Entitlements inspectEntitlements(Principal p,@PathVariable UUID owner) {return service.inspect(UUID.fromString(p.getName()),owner);}
    @PostMapping(value="/admin/entitlements/{owner}",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=GrantWrite.class))))
    public EntitlementService.Entitlements changeEntitlements(Principal p,@PathVariable UUID owner,@RequestBody String body) {
        rates.take("grant:"+p.getName(),30);var in=json.read(body,GrantWrite.class);
        return service.change(UUID.fromString(p.getName()),owner,in.requestId(),in.action(),in.days(),in.expectedRevision());
    }
}

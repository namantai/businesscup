package com.aicompass.compass.portfolio;

import com.aicompass.compass.identity.*;
import com.aicompass.compass.recommendation.OptimizationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static com.aicompass.compass.portfolio.SubscriptionDtos.*;

@RestController
@RequestMapping(value="/api/v1/subscriptions",produces="application/json")
public class SubscriptionController {
    private final SubscriptionService service;
    private final IdentityJson json;
    private final AuthRateLimiter rates;
    public SubscriptionController(SubscriptionService service,IdentityJson json,AuthRateLimiter rates){this.service=service;this.json=json;this.rates=rates;}
    private UUID owner(Principal p){return UUID.fromString(p.getName());}
    @GetMapping public List<SubscriptionEntry> subscriptions(Principal p){return service.subscriptions(owner(p));}
    @GetMapping("/{id}") public SubscriptionEntry subscription(Principal p,@PathVariable UUID id){return service.subscription(owner(p),id);}
    @PostMapping(consumes="application/json") @ResponseStatus(HttpStatus.CREATED)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=SubscriptionWrite.class))))
    public SubscriptionEntry createSubscription(Principal p,@RequestBody String body){return service.write(owner(p),null,json.read(body,SubscriptionWrite.class));}
    @PatchMapping(value="/{id}",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=SubscriptionWrite.class))))
    public SubscriptionEntry updateSubscription(Principal p,@PathVariable UUID id,@RequestBody String body){return service.write(owner(p),id,json.read(body,SubscriptionWrite.class));}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscription(Principal p,@PathVariable UUID id,@RequestParam long revision){service.delete(owner(p),id,revision);}
    @PostMapping(value="/analyze",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=OptimizationRequest.class))))
    public OptimizationResult analyzeSubscriptions(Principal p,@RequestBody String body){rates.take("optimize:"+p.getName(),20);return service.optimize(owner(p),json.read(body,OptimizationRequest.class));}
}

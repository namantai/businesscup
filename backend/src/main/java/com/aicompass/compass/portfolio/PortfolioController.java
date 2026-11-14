package com.aicompass.compass.portfolio;

import com.aicompass.compass.identity.*;
import jakarta.servlet.http.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import static com.aicompass.compass.portfolio.PortfolioDtos.*;

@RestController
@RequestMapping(value="/api/v1",produces="application/json")
public class PortfolioController {
    private final PortfolioService portfolio;
    private final IdentityService identity;
    private final IdentityJson json;
    private final AuthRateLimiter rates;
    public PortfolioController(PortfolioService portfolio,IdentityService identity,IdentityJson json,AuthRateLimiter rates){this.portfolio=portfolio;this.identity=identity;this.json=json;this.rates=rates;}
    private UUID owner(Principal principal){return UUID.fromString(principal.getName());}
    @GetMapping("/me") public AuthDtos.Account me(Principal p){return identity.account(owner(p));}
    @GetMapping("/me/export") public DataExport export(Principal p){UUID id=owner(p);return new DataExport(identity.account(id),portfolio.profiles(id),portfolio.recommendations(id),portfolio.stacks(id));}
    @DeleteMapping(value="/me",consumes="application/json") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=AuthDtos.DeleteRequest.class))))
    public void deleteAccount(Principal p,@RequestBody String body,HttpServletRequest req,HttpServletResponse res){var in=json.read(body,AuthDtos.DeleteRequest.class);rates.take("delete:"+p.getName(),5);identity.delete(owner(p),in.password());new SecurityContextLogoutHandler().logout(req,res,SecurityContextHolder.getContext().getAuthentication());}
    @GetMapping("/profiles") public List<SavedProfile> profiles(Principal p){return portfolio.profiles(owner(p));}
    @GetMapping("/profiles/{id}") public SavedProfile profile(Principal p,@PathVariable UUID id){return portfolio.profile(owner(p),id);}
    @PostMapping(value="/profiles",consumes="application/json") @ResponseStatus(HttpStatus.CREATED)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=ProfileWrite.class))))
    public SavedProfile createProfile(Principal p,@RequestBody String body){return portfolio.create(owner(p),json.read(body,ProfileWrite.class));}
    @PatchMapping(value="/profiles/{id}",consumes="application/json")
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=ProfileWrite.class))))
    public SavedProfile updateProfile(Principal p,@PathVariable UUID id,@RequestBody String body){return portfolio.update(owner(p),id,json.read(body,ProfileWrite.class));}
    @DeleteMapping("/profiles/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteProfile(Principal p,@PathVariable UUID id){portfolio.deleteProfile(owner(p),id);}
    @GetMapping("/recommendations") public List<SavedRecommendation> savedRecommendations(Principal p){return portfolio.recommendations(owner(p));}
    @GetMapping("/recommendations/{id}") public SavedRecommendation savedRecommendation(Principal p,@PathVariable UUID id){return portfolio.recommendation(owner(p),id);}
    @PostMapping(value="/recommendations",consumes="application/json") @ResponseStatus(HttpStatus.CREATED)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=SnapshotWrite.class))))
    public SavedRecommendation saveRecommendation(Principal p,@RequestBody String body){rates.take("save:"+p.getName(),20);return portfolio.save(owner(p),json.read(body,SnapshotWrite.class));}
    @DeleteMapping("/recommendations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteRecommendation(Principal p,@PathVariable UUID id){portfolio.deleteRecommendation(owner(p),id);}
    @GetMapping("/saved-stacks") public List<SavedStack> savedStacks(Principal p){return portfolio.stacks(owner(p));}
    @PostMapping(value="/saved-stacks",consumes="application/json") @ResponseStatus(HttpStatus.CREATED)
    @Operation(requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content=@Content(schema=@Schema(implementation=StackWrite.class))))
    public SavedStack saveStack(Principal p,@RequestBody String body){return portfolio.stack(owner(p),json.read(body,StackWrite.class));}
    @DeleteMapping("/saved-stacks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteStack(Principal p,@PathVariable UUID id){portfolio.deleteStack(owner(p),id);}
}

package com.aicompass.compass.catalog;

import com.aicompass.compass.identity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/admin/catalog", produces = "application/json")
public class CatalogAdminController {
    private final CatalogAdminService admin;
    private final IdentityJson json;
    private final AuthRateLimiter rates;
    public CatalogAdminController(CatalogAdminService admin, IdentityJson json, AuthRateLimiter rates) {
        this.admin = admin; this.json = json; this.rates = rates;
    }
    public record DraftWrite(@NotBlank @Size(max = 5000000) String manifest, @Min(0) long revision) {}
    public record DraftAction(@Pattern(regexp = "VALIDATE|REVIEW|PUBLISH") @NotNull String action,
            @Min(1) long revision, @Min(0) long activeRevision) {}
    private UUID actor(Principal p) { return UUID.fromString(p.getName()); }
    @GetMapping("/drafts") public List<CatalogAdminService.Draft> adminDrafts(Principal p) { return admin.drafts(actor(p)); }
    @GetMapping("/active") public CatalogPublicationService.Active adminActive(Principal p) { return admin.active(actor(p)); }
    @GetMapping("/audit") public List<CatalogAdminService.Audit> adminAudit(Principal p) { return admin.history(actor(p)); }
    @GetMapping("/queue") public List<CatalogViews.Evidence> adminQueue(Principal p) { return admin.queue(actor(p)); }
    @PostMapping(value = "/drafts", consumes = "application/json")
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = DraftWrite.class))))
    public CatalogAdminService.Draft createCatalogDraft(Principal p, @RequestBody String body) {
        rates.take("admin:" + p.getName(), 60);
        return admin.create(actor(p), json.read(body, DraftWrite.class).manifest());
    }
    @PatchMapping(value = "/drafts/{id}", consumes = "application/json")
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = DraftWrite.class))))
    public CatalogAdminService.Draft updateCatalogDraft(Principal p, @PathVariable UUID id, @RequestBody String body) {
        rates.take("admin:" + p.getName(), 60);
        var in = json.read(body, DraftWrite.class);
        return admin.edit(actor(p), id, in.revision(), in.manifest());
    }
    @PostMapping(value = "/drafts/{id}/actions", consumes = "application/json")
    @Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = DraftAction.class))))
    public CatalogAdminService.Draft advanceCatalogDraft(Principal p, @PathVariable UUID id, @RequestBody String body) {
        rates.take("admin:" + p.getName(), 60);
        var in = json.read(body, DraftAction.class);
        return admin.advance(actor(p), id, in.revision(), in.action(), in.activeRevision());
    }
}

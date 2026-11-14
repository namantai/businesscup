package com.aicompass.compass.catalog;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Published catalog data", useReturnTypeSchema = true),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/problem+json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.aicompass.compass.shared.web.ApiProblem.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Published entry not found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/problem+json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.aicompass.compass.shared.web.ApiProblem.class)))
})
@RestController
@RequestMapping(value = "/api/v1/catalog", produces = "application/json")
public class CatalogController {
    private final CatalogReadService catalog;
    public CatalogController(CatalogReadService catalog) { this.catalog = catalog; }

    @GetMapping("/meta")
    public CatalogViews.Meta meta() { return catalog.meta(); }

    @GetMapping("/products/{slug}")
    public CatalogViews.Detail detail(@PathVariable String slug, @RequestParam(required = false) UUID version) {
        return catalog.detail(slug, version);
    }

    @GetMapping("/evidence")
    public CatalogViews.Page<CatalogViews.Evidence> evidence(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.evidencePage(version, page, size, false, CatalogViews.Evidence.class);
    }
    @GetMapping("/sources")
    public CatalogViews.Page<CatalogViews.Source> sources(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.evidencePage(version, page, size, true, CatalogViews.Source.class);
    }
    @GetMapping("/products")
    public CatalogViews.Page<CatalogManifest.Product> products(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("products", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.Product.class);
    }
    @GetMapping("/providers")
    public CatalogViews.Page<CatalogManifest.Provider> providers(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("providers", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.Provider.class);
    }
    @GetMapping("/regions")
    public CatalogViews.Page<CatalogManifest.Region> regions(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("regions", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.Region.class);
    }
    @GetMapping("/models")
    public CatalogViews.Page<CatalogManifest.AiModel> models(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("models", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.AiModel.class);
    }
    @GetMapping("/model-families")
    public CatalogViews.Page<CatalogManifest.ModelFamily> modelFamilies(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("model-families", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.ModelFamily.class);
    }
    @GetMapping("/agents")
    public CatalogViews.Page<CatalogManifest.Agent> agents(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("agents", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.Agent.class);
    }
    @GetMapping("/plans")
    public CatalogViews.Page<CatalogManifest.SubscriptionPlan> plans(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("plans", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.SubscriptionPlan.class);
    }
    @GetMapping("/offers")
    public CatalogViews.Page<CatalogViews.Offer> offers(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("offers", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogViews.Offer.class);
    }
    @GetMapping("/needs")
    public CatalogViews.Page<CatalogManifest.UserNeed> needs(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("needs", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.UserNeed.class);
    }
    @GetMapping("/capabilities")
    public CatalogViews.Page<CatalogManifest.Capability> capabilities(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("capabilities", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.Capability.class);
    }
    @GetMapping("/need-capabilities")
    public CatalogViews.Page<CatalogManifest.NeedCapability> needCapabilities(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("need-capabilities", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.NeedCapability.class);
    }
    @GetMapping("/assertions")
    public CatalogViews.Page<CatalogManifest.CapabilityAssertion> assertions(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("assertions", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.CapabilityAssertion.class);
    }
    @GetMapping("/model-access")
    public CatalogViews.Page<CatalogManifest.PlanModelAccess> modelAccess(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("model-access", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.PlanModelAccess.class);
    }
    @GetMapping("/agent-access")
    public CatalogViews.Page<CatalogManifest.PlanAgentAccess> agentAccess(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("agent-access", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.PlanAgentAccess.class);
    }
    @GetMapping("/quota-pools")
    public CatalogViews.Page<CatalogManifest.QuotaPool> quotaPools(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("quota-pools", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.QuotaPool.class);
    }
    @GetMapping("/usage-limits")
    public CatalogViews.Page<CatalogManifest.UsageLimit> usageLimits(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("usage-limits", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.UsageLimit.class);
    }
    @GetMapping("/eligibility-rules")
    public CatalogViews.Page<CatalogManifest.EligibilityRule> eligibilityRules(@RequestParam(required = false) UUID version,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String kind,
            @RequestParam(required = false) String productId, @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String currency, @RequestParam(required = false) String capabilityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {
        return catalog.typedPage("eligibility-rules", version, q, kind, productId, regionId, currency, capabilityId, page, size, sort, CatalogManifest.EligibilityRule.class);
    }
}

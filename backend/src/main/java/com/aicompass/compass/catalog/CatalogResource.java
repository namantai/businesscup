package com.aicompass.compass.catalog;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Closed SQL allowlist. No request input can supply a table or column name. */
public enum CatalogResource {
    PRODUCT("products", "product", "id, provider_id, slug, name, kind, state, metadata_evidence_id", CatalogManifest.Product.class),
    PROVIDER("providers", "provider", "id, name, official_domain", CatalogManifest.Provider.class),
    REGION("regions", "region", "id, country_code, name", CatalogManifest.Region.class),
    AI_MODEL("models", "ai_model", "id, family_id, name, exact_version", CatalogManifest.AiModel.class),
    MODEL_FAMILY("model-families", "model_family", "id, provider_id, name", CatalogManifest.ModelFamily.class),
    AGENT("agents", "agent", "id, provider_id, product_id, name, execution_environment, metadata_evidence_id", CatalogManifest.Agent.class),
    SUBSCRIPTION_PLAN("plans", "subscription_plan", "id, product_id, tier, access_type, exclusivity_group_id, active_from, active_until", CatalogManifest.SubscriptionPlan.class),
    PRICE_OFFER("offers", "price_offer", "id, plan_id, region_id, channel, currency, price_kind, billing_period, amount, upfront_amount, usage_unit, tax_status, additional_tax, fee_status, additional_fee, trial_days, conditions, valid_from, valid_until, price_evidence_id, tax_evidence_id, fee_evidence_id, region_evidence_id", CatalogManifest.PriceOffer.class),
    USER_NEED("needs", "user_need", "id, name", CatalogManifest.UserNeed.class),
    CAPABILITY("capabilities", "capability", "id, category, name", CatalogManifest.Capability.class),
    NEED_CAPABILITY("need-capabilities", "need_capability", "id, need_id, capability_id", CatalogManifest.NeedCapability.class),
    CAPABILITY_ASSERTION("assertions", "capability_assertion", "id, product_id, plan_id, capability_id, state, conditions, evidence_id", CatalogManifest.CapabilityAssertion.class),
    PLAN_MODEL_ACCESS("model-access", "plan_model_access", "id, plan_id, model_id, mode, state, conditions, evidence_id", CatalogManifest.PlanModelAccess.class),
    PLAN_AGENT_ACCESS("agent-access", "plan_agent_access", "id, plan_id, agent_id, state, conditions, evidence_id", CatalogManifest.PlanAgentAccess.class),
    QUOTA_POOL("quota-pools", "quota_pool", "id, offer_id, state, quantity, unit, reset_period, expires_at, conditions, evidence_id", CatalogManifest.QuotaPool.class),
    USAGE_LIMIT("usage-limits", "usage_limit", "id, offer_id, capability_id, pool_id, mode, units_per_use", CatalogManifest.UsageLimit.class),
    ELIGIBILITY_RULE("eligibility-rules", "eligibility_rule", "id, offer_id, kind, state, requirement, conditions, evidence_id", CatalogManifest.EligibilityRule.class);
    public final String path;
    public final String table;
    public final String columns;
    public final Class<?> type;
    CatalogResource(String path, String table, String columns, Class<?> type) {
        this.path = path; this.table = table; this.columns = columns; this.type = type;
    }
    public static CatalogResource from(String path) {
        return Arrays.stream(values()).filter(r -> r.path.equals(path)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog resource not found"));
    }
}

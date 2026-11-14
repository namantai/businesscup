package com.aicompass.compass.catalog;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogImportService {
    private final JdbcTemplate jdbc;
    private final CatalogJson json;
    private final CatalogImportValidator validator;
    private final CatalogPolicy policy;
    private final Clock clock;

    public CatalogImportService(JdbcTemplate jdbc, CatalogJson json, CatalogImportValidator validator,
                                CatalogPolicy policy, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.validator = validator;
        this.policy = policy;
        this.clock = clock;
    }

    /** Local application boundary only: no public HTTP endpoint or automatic seed. */
    @Transactional
    public UUID importManifest(String input) {
        CatalogManifest m = json.read(input);
        validator.validate(m, policy.allowDemo());
        String artifact = json.write(m);
        String hash = sha256(artifact);
        UUID version = m.versionId();
        // Concurrent identical imports can race on either unique index (id or hash).
        // Handle both, then verify the exact immutable id/hash below.
        int inserted = jdbc.update("""
                INSERT INTO compass.catalog_version
                    (id, schema_version, dataset_kind, state, content_hash, as_of, reviewer, manifest_json)
                VALUES (?, ?, ?, 'DRAFT', ?, ?, ?, ?) ON CONFLICT DO NOTHING
                """, version, m.schemaVersion(), m.datasetKind().name(), hash, timestamp(m.asOf()), m.reviewer(), artifact);
        if (inserted == 0) {
            String existing = jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id = ? AND state = 'PUBLISHED'", String.class, version);
            if (!hash.equals(existing)) throw new IllegalArgumentException("Version id already exists with different content");
            return version;
        }
        m.providers().forEach(r -> jdbc.update("""
                INSERT INTO compass.provider (version_id, id, name, official_domain)
                VALUES (?, ?, ?, ?)
                """, version, r.id(), r.name(), r.officialDomain()));
        m.regions().forEach(r -> jdbc.update("""
                INSERT INTO compass.region (version_id, id, country_code, name)
                VALUES (?, ?, ?, ?)
                """, version, r.id(), r.countryCode(), r.name()));
        m.evidenceSources().forEach(r -> jdbc.update("""
                INSERT INTO compass.evidence_source (version_id, id, url, publisher, kind, usage_rights)
                VALUES (?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.url(), r.publisher(), r.kind().name(), r.usageRights()));
        m.evidence().forEach(r -> jdbc.update("""
                INSERT INTO compass.evidence (version_id, id, source_id, claim_type, status, verified_at, observed_region_id, channel, summary, reviewer, method, conditions)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.sourceId(), r.claimType().name(), r.status().name(), timestamp(r.verifiedAt()), r.observedRegionId(), r.channel(), r.summary(), r.reviewer(), r.method(), r.conditions()));
        m.products().forEach(r -> jdbc.update("""
                INSERT INTO compass.product (version_id, id, provider_id, slug, name, kind, state, metadata_evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.providerId(), r.slug(), r.name(), r.kind().name(), r.state().name(), r.metadataEvidenceId()));
        m.modelFamilies().forEach(r -> jdbc.update("""
                INSERT INTO compass.model_family (version_id, id, provider_id, name)
                VALUES (?, ?, ?, ?)
                """, version, r.id(), r.providerId(), r.name()));
        m.aiModels().forEach(r -> jdbc.update("""
                INSERT INTO compass.ai_model (version_id, id, family_id, name, exact_version)
                VALUES (?, ?, ?, ?, ?)
                """, version, r.id(), r.familyId(), r.name(), r.exactVersion()));
        m.agents().forEach(r -> jdbc.update("""
                INSERT INTO compass.agent (version_id, id, provider_id, product_id, name, execution_environment, metadata_evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.providerId(), r.productId(), r.name(), r.executionEnvironment(), r.metadataEvidenceId()));
        m.capabilities().forEach(r -> jdbc.update("""
                INSERT INTO compass.capability (version_id, id, category, name)
                VALUES (?, ?, ?, ?)
                """, version, r.id(), r.category().name(), r.name()));
        m.userNeeds().forEach(r -> jdbc.update("""
                INSERT INTO compass.user_need (version_id, id, name)
                VALUES (?, ?, ?)
                """, version, r.id(), r.name()));
        m.needCapabilities().forEach(r -> jdbc.update("""
                INSERT INTO compass.need_capability (version_id, id, need_id, capability_id)
                VALUES (?, ?, ?, ?)
                """, version, r.id(), r.needId(), r.capabilityId()));
        m.exclusivityGroups().forEach(r -> jdbc.update("""
                INSERT INTO compass.exclusivity_group (version_id, id, product_id, name)
                VALUES (?, ?, ?, ?)
                """, version, r.id(), r.productId(), r.name()));
        m.subscriptionPlans().forEach(r -> jdbc.update("""
                INSERT INTO compass.subscription_plan (version_id, id, product_id, tier, access_type, exclusivity_group_id, active_from, active_until)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.productId(), r.tier(), r.accessType().name(), r.exclusivityGroupId(), timestamp(r.activeFrom()), timestamp(r.activeUntil())));
        m.priceOffers().forEach(r -> jdbc.update("""
                INSERT INTO compass.price_offer (version_id, id, plan_id, region_id, channel, currency, price_kind, billing_period, amount, upfront_amount, usage_unit, tax_status, additional_tax, fee_status, additional_fee, trial_days, conditions, valid_from, valid_until, price_evidence_id, tax_evidence_id, fee_evidence_id, region_evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.planId(), r.regionId(), r.channel(), r.currency(), r.priceKind().name(), r.billingPeriod().name(), decimal(r.amount()), decimal(r.upfrontAmount()), r.usageUnit(), r.taxStatus().name(), decimal(r.additionalTax()), r.feeStatus().name(), decimal(r.additionalFee()), r.trialDays(), r.conditions(), timestamp(r.validFrom()), timestamp(r.validUntil()), r.priceEvidenceId(), r.taxEvidenceId(), r.feeEvidenceId(), r.regionEvidenceId()));
        m.planModelAccess().forEach(r -> jdbc.update("""
                INSERT INTO compass.plan_model_access (version_id, id, plan_id, model_id, mode, state, conditions, evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.planId(), r.modelId(), r.mode(), r.state().name(), r.conditions(), r.evidenceId()));
        m.planAgentAccess().forEach(r -> jdbc.update("""
                INSERT INTO compass.plan_agent_access (version_id, id, plan_id, agent_id, state, conditions, evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.planId(), r.agentId(), r.state().name(), r.conditions(), r.evidenceId()));
        m.capabilityAssertions().forEach(r -> jdbc.update("""
                INSERT INTO compass.capability_assertion (version_id, id, product_id, plan_id, capability_id, state, conditions, evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.productId(), r.planId(), r.capabilityId(), r.state().name(), r.conditions(), r.evidenceId()));
        m.quotaPools().forEach(r -> jdbc.update("""
                INSERT INTO compass.quota_pool (version_id, id, offer_id, state, quantity, unit, reset_period, expires_at, conditions, evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.offerId(), r.state().name(), decimal(r.quantity()), r.unit(), r.resetPeriod().name(), timestamp(r.expiresAt()), r.conditions(), r.evidenceId()));
        m.usageLimits().forEach(r -> jdbc.update("""
                INSERT INTO compass.usage_limit (version_id, id, offer_id, capability_id, pool_id, mode, units_per_use)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.offerId(), r.capabilityId(), r.poolId(), r.mode(), decimal(r.unitsPerUse())));
        m.eligibilityRules().forEach(r -> jdbc.update("""
                INSERT INTO compass.eligibility_rule (version_id, id, offer_id, kind, state, requirement, conditions, evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.offerId(), r.kind().name(), r.state().name(), r.requirement(), r.conditions(), r.evidenceId()));
        m.benchmarkObservations().forEach(r -> jdbc.update("""
                INSERT INTO compass.benchmark_observation (version_id, id, model_id, task, metric, value, unit, methodology, observed_at, evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.modelId(), r.task(), r.metric(), decimal(r.value()), r.unit(), r.methodology(), timestamp(r.observedAt()), r.evidenceId()));
        m.fxSnapshots().forEach(r -> jdbc.update("""
                INSERT INTO compass.fx_snapshot (version_id, id, base_currency, quote_currency, rate, observed_at, evidence_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, version, r.id(), r.baseCurrency(), r.quoteCurrency(), decimal(r.rate()), timestamp(r.observedAt()), r.evidenceId()));
        jdbc.update("UPDATE compass.catalog_version SET state = 'PUBLISHED', published_at = ? WHERE id = ?",
                timestamp(clock.instant()), version);
        return version;
    }

    @Transactional(readOnly = true)
    public CatalogManifest readPublishedArtifact(UUID id) {
        String artifact = jdbc.queryForObject("""
                SELECT manifest_json FROM compass.catalog_version
                WHERE id = ? AND state = 'PUBLISHED' AND (dataset_kind = 'REVIEWED' OR ?)
                """, String.class, id, policy.allowDemo());
        return json.read(artifact);
    }

    private static BigDecimal decimal(String value) { return value == null ? null : new BigDecimal(value); }
    private static OffsetDateTime timestamp(Instant value) { return value == null ? null : value.atOffset(ZoneOffset.UTC); }
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }
}

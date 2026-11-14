package com.aicompass.compass.catalog;

import com.aicompass.compass.shared.money.Money;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import static com.aicompass.compass.catalog.CatalogTypes.*;

@Component
public class CatalogImportValidator {
    private final Validator beanValidator;
    private final Clock clock;

    public CatalogImportValidator(Validator beanValidator, Clock clock) {
        this.beanValidator = beanValidator;
        this.clock = clock;
    }

    public void validate(CatalogManifest m, boolean allowDemo) {
        Objects.requireNonNull(m, "manifest");
        List<String> errors = new ArrayList<>();
        beanValidator.validate(m).forEach(v -> errors.add(v.getPropertyPath() + ": " + v.getMessage()));
        fail(errors);
        check(!m.asOf().isAfter(clock.instant()), "asOf: cannot be in the future", errors);
        check(m.datasetKind() != DatasetKind.DEMO || allowDemo, "datasetKind: DEMO disabled outside explicit dev/test", errors);
        var providers = index(m.providers(), CatalogManifest.Provider::id, "providers", errors);
        var regions = index(m.regions(), CatalogManifest.Region::id, "regions", errors);
        var evidenceSources = index(m.evidenceSources(), CatalogManifest.EvidenceSource::id, "evidenceSources", errors);
        var evidence = index(m.evidence(), CatalogManifest.Evidence::id, "evidence", errors);
        var products = index(m.products(), CatalogManifest.Product::id, "products", errors);
        var modelFamilies = index(m.modelFamilies(), CatalogManifest.ModelFamily::id, "modelFamilies", errors);
        var aiModels = index(m.aiModels(), CatalogManifest.AiModel::id, "aiModels", errors);
        var agents = index(m.agents(), CatalogManifest.Agent::id, "agents", errors);
        var capabilities = index(m.capabilities(), CatalogManifest.Capability::id, "capabilities", errors);
        var userNeeds = index(m.userNeeds(), CatalogManifest.UserNeed::id, "userNeeds", errors);
        var needCapabilities = index(m.needCapabilities(), CatalogManifest.NeedCapability::id, "needCapabilities", errors);
        var exclusivityGroups = index(m.exclusivityGroups(), CatalogManifest.ExclusivityGroup::id, "exclusivityGroups", errors);
        var subscriptionPlans = index(m.subscriptionPlans(), CatalogManifest.SubscriptionPlan::id, "subscriptionPlans", errors);
        var priceOffers = index(m.priceOffers(), CatalogManifest.PriceOffer::id, "priceOffers", errors);
        var planModelAccess = index(m.planModelAccess(), CatalogManifest.PlanModelAccess::id, "planModelAccess", errors);
        var planAgentAccess = index(m.planAgentAccess(), CatalogManifest.PlanAgentAccess::id, "planAgentAccess", errors);
        var capabilityAssertions = index(m.capabilityAssertions(), CatalogManifest.CapabilityAssertion::id, "capabilityAssertions", errors);
        var quotaPools = index(m.quotaPools(), CatalogManifest.QuotaPool::id, "quotaPools", errors);
        var usageLimits = index(m.usageLimits(), CatalogManifest.UsageLimit::id, "usageLimits", errors);
        var eligibilityRules = index(m.eligibilityRules(), CatalogManifest.EligibilityRule::id, "eligibilityRules", errors);
        var benchmarkObservations = index(m.benchmarkObservations(), CatalogManifest.BenchmarkObservation::id, "benchmarkObservations", errors);
        var fxSnapshots = index(m.fxSnapshots(), CatalogManifest.FxSnapshot::id, "fxSnapshots", errors);
        m.evidence().forEach(r -> reference(r.sourceId(), evidenceSources, "evidence." + r.id() + ".sourceId", errors));
        m.evidence().forEach(r -> reference(r.observedRegionId(), regions, "evidence." + r.id() + ".observedRegionId", errors));
        m.products().forEach(r -> reference(r.providerId(), providers, "products." + r.id() + ".providerId", errors));
        m.products().forEach(r -> reference(r.metadataEvidenceId(), evidence, "products." + r.id() + ".metadataEvidenceId", errors));
        m.modelFamilies().forEach(r -> reference(r.providerId(), providers, "modelFamilies." + r.id() + ".providerId", errors));
        m.aiModels().forEach(r -> reference(r.familyId(), modelFamilies, "aiModels." + r.id() + ".familyId", errors));
        m.agents().forEach(r -> reference(r.providerId(), providers, "agents." + r.id() + ".providerId", errors));
        m.agents().forEach(r -> reference(r.productId(), products, "agents." + r.id() + ".productId", errors));
        m.agents().forEach(r -> reference(r.metadataEvidenceId(), evidence, "agents." + r.id() + ".metadataEvidenceId", errors));
        m.needCapabilities().forEach(r -> reference(r.needId(), userNeeds, "needCapabilities." + r.id() + ".needId", errors));
        m.needCapabilities().forEach(r -> reference(r.capabilityId(), capabilities, "needCapabilities." + r.id() + ".capabilityId", errors));
        m.exclusivityGroups().forEach(r -> reference(r.productId(), products, "exclusivityGroups." + r.id() + ".productId", errors));
        m.subscriptionPlans().forEach(r -> reference(r.productId(), products, "subscriptionPlans." + r.id() + ".productId", errors));
        m.priceOffers().forEach(r -> reference(r.planId(), subscriptionPlans, "priceOffers." + r.id() + ".planId", errors));
        m.priceOffers().forEach(r -> reference(r.regionId(), regions, "priceOffers." + r.id() + ".regionId", errors));
        m.priceOffers().forEach(r -> reference(r.priceEvidenceId(), evidence, "priceOffers." + r.id() + ".priceEvidenceId", errors));
        m.priceOffers().forEach(r -> reference(r.taxEvidenceId(), evidence, "priceOffers." + r.id() + ".taxEvidenceId", errors));
        m.priceOffers().forEach(r -> reference(r.feeEvidenceId(), evidence, "priceOffers." + r.id() + ".feeEvidenceId", errors));
        m.priceOffers().forEach(r -> reference(r.regionEvidenceId(), evidence, "priceOffers." + r.id() + ".regionEvidenceId", errors));
        m.planModelAccess().forEach(r -> reference(r.planId(), subscriptionPlans, "planModelAccess." + r.id() + ".planId", errors));
        m.planModelAccess().forEach(r -> reference(r.modelId(), aiModels, "planModelAccess." + r.id() + ".modelId", errors));
        m.planModelAccess().forEach(r -> reference(r.evidenceId(), evidence, "planModelAccess." + r.id() + ".evidenceId", errors));
        m.planAgentAccess().forEach(r -> reference(r.planId(), subscriptionPlans, "planAgentAccess." + r.id() + ".planId", errors));
        m.planAgentAccess().forEach(r -> reference(r.agentId(), agents, "planAgentAccess." + r.id() + ".agentId", errors));
        m.planAgentAccess().forEach(r -> reference(r.evidenceId(), evidence, "planAgentAccess." + r.id() + ".evidenceId", errors));
        m.capabilityAssertions().forEach(r -> reference(r.productId(), products, "capabilityAssertions." + r.id() + ".productId", errors));
        m.capabilityAssertions().forEach(r -> reference(r.planId(), subscriptionPlans, "capabilityAssertions." + r.id() + ".planId", errors));
        m.capabilityAssertions().forEach(r -> reference(r.capabilityId(), capabilities, "capabilityAssertions." + r.id() + ".capabilityId", errors));
        m.capabilityAssertions().forEach(r -> reference(r.evidenceId(), evidence, "capabilityAssertions." + r.id() + ".evidenceId", errors));
        m.quotaPools().forEach(r -> reference(r.offerId(), priceOffers, "quotaPools." + r.id() + ".offerId", errors));
        m.quotaPools().forEach(r -> reference(r.evidenceId(), evidence, "quotaPools." + r.id() + ".evidenceId", errors));
        m.usageLimits().forEach(r -> reference(r.offerId(), priceOffers, "usageLimits." + r.id() + ".offerId", errors));
        m.usageLimits().forEach(r -> reference(r.capabilityId(), capabilities, "usageLimits." + r.id() + ".capabilityId", errors));
        m.eligibilityRules().forEach(r -> reference(r.offerId(), priceOffers, "eligibilityRules." + r.id() + ".offerId", errors));
        m.eligibilityRules().forEach(r -> reference(r.evidenceId(), evidence, "eligibilityRules." + r.id() + ".evidenceId", errors));
        m.benchmarkObservations().forEach(r -> reference(r.modelId(), aiModels, "benchmarkObservations." + r.id() + ".modelId", errors));
        m.benchmarkObservations().forEach(r -> reference(r.evidenceId(), evidence, "benchmarkObservations." + r.id() + ".evidenceId", errors));
        m.fxSnapshots().forEach(r -> reference(r.evidenceId(), evidence, "fxSnapshots." + r.id() + ".evidenceId", errors));
        fail(errors);
        m.evidenceSources().forEach(source -> {
            URI uri;
            try { uri = URI.create(source.url()); }
            catch (IllegalArgumentException ex) { errors.add("evidenceSources." + source.id() + ": invalid URL"); return; }
            check("https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null,
                    "evidenceSources." + source.id() + ": HTTPS URL without credentials required", errors);
            check(m.datasetKind() != DatasetKind.REVIEWED || source.kind() != SourceKind.SYNTHETIC,
                    "evidenceSources." + source.id() + ": synthetic source in reviewed catalog", errors);
            check(m.datasetKind() != DatasetKind.DEMO || (source.kind() == SourceKind.SYNTHETIC
                    && uri.getHost() != null && uri.getHost().endsWith(".invalid")),
                    "evidenceSources." + source.id() + ": demo requires synthetic .invalid source", errors);
        });
        m.evidence().forEach(e -> {
            check(e.status() == EvidenceStatus.UNKNOWN || (e.sourceId() != null && e.verifiedAt() != null),
                    "evidence." + e.id() + ": non-unknown claim requires source and verification date", errors);
            check(e.verifiedAt() == null || !e.verifiedAt().isAfter(m.asOf()), "evidence." + e.id() + ": future verification", errors);
            check(m.datasetKind() != DatasetKind.REVIEWED || e.status() != EvidenceStatus.DEMO,
                    "evidence." + e.id() + ": demo claim in reviewed catalog", errors);
            check(m.datasetKind() != DatasetKind.DEMO || e.status() != EvidenceStatus.VERIFIED_SOURCE,
                    "evidence." + e.id() + ": demo cannot claim real-world verification", errors);
        });
        m.products().forEach(r -> claim(evidence, r.metadataEvidenceId(), ClaimType.PRODUCT_METADATA, r.id(), errors));
        m.agents().forEach(r -> {
            claim(evidence, r.metadataEvidenceId(), ClaimType.PRODUCT_METADATA, r.id(), errors);
            check(r.productId() == null || products.get(r.productId()).providerId().equals(r.providerId()),
                    "agents." + r.id() + ": provider differs from associated product", errors);
        });
        m.subscriptionPlans().forEach(p -> {
            reference(p.exclusivityGroupId(), exclusivityGroups, "plans." + p.id() + ".exclusivityGroupId", errors);
            if (p.exclusivityGroupId() != null && exclusivityGroups.containsKey(p.exclusivityGroupId())) {
                check(exclusivityGroups.get(p.exclusivityGroupId()).productId().equals(p.productId()),
                        "plans." + p.id() + ": exclusivity group belongs to another product", errors);
            }
            interval(p.activeFrom(), p.activeUntil(), "plans." + p.id(), errors);
        });
        m.priceOffers().forEach(o -> {
            validatePrice(o, errors);
            claim(evidence, o.priceEvidenceId(), ClaimType.PRICE, o.id(), errors);
            claim(evidence, o.taxEvidenceId(), ClaimType.TAX, o.id(), errors);
            claim(evidence, o.feeEvidenceId(), ClaimType.FEES, o.id(), errors);
            claim(evidence, o.regionEvidenceId(), ClaimType.REGION, o.id(), errors);
            check(evidence.get(o.priceEvidenceId()).status() != EvidenceStatus.UNKNOWN || o.priceKind() == PriceKind.UNKNOWN,
                    "offers." + o.id() + ": UNKNOWN evidence cannot become a known/free price", errors);
            check(evidence.get(o.taxEvidenceId()).status() != EvidenceStatus.UNKNOWN || o.taxStatus() == TaxStatus.UNKNOWN,
                    "offers." + o.id() + ": UNKNOWN tax evidence cannot establish a known tax", errors);
            check(evidence.get(o.feeEvidenceId()).status() != EvidenceStatus.UNKNOWN || o.feeStatus() == ValueStatus.UNKNOWN,
                    "offers." + o.id() + ": UNKNOWN fee evidence cannot establish a known fee", errors);
            interval(o.validFrom(), o.validUntil(), "offers." + o.id(), errors);
        });
        m.planModelAccess().forEach(r -> {
            claim(evidence, r.evidenceId(), ClaimType.MODEL_ACCESS, r.id(), errors);
            knownState(evidence.get(r.evidenceId()), r.state().name(), r.id(), errors);
        });
        m.planAgentAccess().forEach(r -> {
            claim(evidence, r.evidenceId(), ClaimType.AGENT_ACCESS, r.id(), errors);
            knownState(evidence.get(r.evidenceId()), r.state().name(), r.id(), errors);
        });
        m.capabilityAssertions().forEach(r -> {
            check((r.productId() == null) != (r.planId() == null), "assertions." + r.id() + ": exactly one product/plan target required", errors);
            claim(evidence, r.evidenceId(), ClaimType.CAPABILITY, r.id(), errors);
            check(r.state() == CapabilityState.UNKNOWN || evidence.get(r.evidenceId()).status() != EvidenceStatus.UNKNOWN,
                    "assertions." + r.id() + ": unknown evidence cannot establish support/absence", errors);
        });
        m.quotaPools().forEach(r -> {
            check((r.state() == LimitState.KNOWN) == (r.quantity() != null), "pools." + r.id() + ": unknown/unlimited is not zero", errors);
            check(r.resetPeriod() != ResetPeriod.NEVER || r.expiresAt() != null, "pools." + r.id() + ": one-time grant requires expiry", errors);
            claim(evidence, r.evidenceId(), ClaimType.USAGE_LIMIT, r.id(), errors);
            knownState(evidence.get(r.evidenceId()), r.state().name(), r.id(), errors);
        });
        m.usageLimits().forEach(r -> {
            reference(r.poolId(), quotaPools, "limits." + r.id() + ".poolId", errors);
            if (quotaPools.containsKey(r.poolId())) check(quotaPools.get(r.poolId()).offerId().equals(r.offerId()),
                    "limits." + r.id() + ": pool must belong to the same offer", errors);
            check(r.unitsPerUse() == null || new BigDecimal(r.unitsPerUse()).signum() > 0,
                    "limits." + r.id() + ": unitsPerUse must be positive", errors);
        });
        m.eligibilityRules().forEach(r -> {
            claim(evidence, r.evidenceId(), ClaimType.ELIGIBILITY, r.id(), errors);
            knownState(evidence.get(r.evidenceId()), r.state().name(), r.id(), errors);
        });
        m.benchmarkObservations().forEach(r -> {
            claim(evidence, r.evidenceId(), ClaimType.BENCHMARK, r.id(), errors);
            knownState(evidence.get(r.evidenceId()), "KNOWN", r.id(), errors);
            check(!r.observedAt().isAfter(m.asOf()), "benchmarks." + r.id() + ": future observation", errors);
            var e = evidence.get(r.evidenceId());
            check(e.sourceId() != null && (evidenceSources.get(e.sourceId()).kind() == SourceKind.INDEPENDENT_BENCHMARK
                    || (m.datasetKind() == DatasetKind.DEMO && evidenceSources.get(e.sourceId()).kind() == SourceKind.SYNTHETIC)),
                    "benchmarks." + r.id() + ": independent methodology source required", errors);
        });
        m.fxSnapshots().forEach(r -> {
            try {
                Money.of("0", r.baseCurrency()); Money.of("0", r.quoteCurrency());
            } catch (IllegalArgumentException ex) { errors.add("fx." + r.id() + ": invalid monetary currency"); }
            check(!r.baseCurrency().equals(r.quoteCurrency()) && new BigDecimal(r.rate()).signum() > 0,
                    "fx." + r.id() + ": positive rate and distinct currencies required", errors);
            check(!r.observedAt().isAfter(m.asOf()), "fx." + r.id() + ": future rate", errors);
            claim(evidence, r.evidenceId(), ClaimType.FX, r.id(), errors);
            knownState(evidence.get(r.evidenceId()), "KNOWN", r.id(), errors);
        });
        fail(errors);
    }

    private static void validatePrice(CatalogManifest.PriceOffer o, List<String> errors) {
        String path = "offers." + o.id();
        try {
            Money.of("0", o.currency());
            if (o.amount() != null) Money.of(o.amount(), o.currency());
            if (o.upfrontAmount() != null) Money.of(o.upfrontAmount(), o.currency());
        } catch (IllegalArgumentException ex) { errors.add(path + ": invalid monetary amount/currency"); }
        BigDecimal amount = decimal(o.amount());
        BigDecimal upfront = decimal(o.upfrontAmount());
        boolean valid = switch (o.priceKind()) {
            case UNKNOWN -> amount == null && upfront == null;
            case FREE -> zero(amount) && zero(upfront) && o.billingPeriod() == BillingPeriod.MONTH;
            case FIXED -> amount != null && amount.signum() > 0 && upfront != null && upfront.compareTo(amount) >= 0
                    && Set.of(BillingPeriod.MONTH, BillingPeriod.YEAR, BillingPeriod.ONE_TIME).contains(o.billingPeriod());
            case TRIAL -> amount != null && upfront != null && upfront.compareTo(amount) >= 0 && o.billingPeriod() == BillingPeriod.ONE_TIME;
            case USAGE_BASED -> amount != null && amount.signum() > 0 && upfront == null && o.billingPeriod() == BillingPeriod.USAGE;
        };
        check(valid, path + ": inconsistent price kind, amount, billing period or full upfront", errors);
        check((o.priceKind() == PriceKind.TRIAL) == (o.trialDays() != null), path + ": trial duration required only for trial", errors);
        check((o.priceKind() == PriceKind.USAGE_BASED) == (o.usageUnit() != null), path + ": usage unit required only for metered price", errors);
        boolean taxValid = switch (o.taxStatus()) {
            case UNKNOWN -> o.additionalTax() == null;
            case INCLUDED, NOT_APPLICABLE -> zero(decimal(o.additionalTax()));
            case EXCLUDED_KNOWN -> o.additionalTax() != null;
        };
        check(taxValid, path + ": unknown tax cannot be zero; included tax cannot be added twice", errors);
        check((o.feeStatus() == ValueStatus.KNOWN) == (o.additionalFee() != null), path + ": unknown fee cannot be zero", errors);
    }

    private static void knownState(CatalogManifest.Evidence evidence, String state, String path, List<String> errors) {
        check(evidence.status() != EvidenceStatus.UNKNOWN || state.equals("UNKNOWN"), path + ": UNKNOWN evidence cannot establish known state", errors);
    }
    private static BigDecimal decimal(String value) { return value == null ? null : new BigDecimal(value); }
    private static boolean zero(BigDecimal value) { return value != null && value.signum() == 0; }
    private static void interval(Instant start, Instant end, String path, List<String> errors) {
        check(end == null || end.isAfter(start), path + ": invalid validity interval", errors);
    }
    private static void claim(Map<String, CatalogManifest.Evidence> evidence, String id, ClaimType type, String path, List<String> errors) {
        check(evidence.get(id).claimType() == type, path + ": evidence does not support field " + type, errors);
    }
    private static <T> Map<String, T> index(List<T> rows, Function<T, String> id, String path, List<String> errors) {
        Map<String, T> index = new HashMap<>();
        rows.forEach(row -> check(index.putIfAbsent(id.apply(row), row) == null, path + ": duplicate id " + id.apply(row), errors));
        return index;
    }
    private static void reference(String id, Map<String, ?> target, String path, List<String> errors) {
        check(id == null || target.containsKey(id), path + ": broken reference", errors);
    }
    private static void check(boolean condition, String error, List<String> errors) {
        if (!condition) errors.add(error);
    }
    private static void fail(List<String> errors) {
        if (!errors.isEmpty()) throw new IllegalArgumentException("Catalog validation failed: " + String.join("; ", new TreeSet<>(errors)));
    }
}

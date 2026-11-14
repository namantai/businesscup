package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.time.Instant;
import java.util.*;
import static com.aicompass.compass.catalog.CatalogTypes.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;
import static com.aicompass.compass.recommendation.CostCalculator.reason;

final class EligibilityAssessment {
    record Assessment(Eligibility status, List<Reason> reasons) {}
    private final PublishedCatalogSnapshot catalog;
    private final RecommendationProfile profile;
    private final EvidenceAssessment evidence;
    private final CapabilityCoverage coverage;
    private final Instant asOf;
    EligibilityAssessment(PublishedCatalogSnapshot catalog, RecommendationProfile profile, EvidenceAssessment evidence, CapabilityCoverage coverage, Instant asOf) {
        this.catalog = catalog; this.profile = profile; this.evidence = evidence; this.coverage = coverage; this.asOf = asOf;
    }
    Assessment assess(CatalogManifest.SubscriptionPlan plan, CatalogManifest.PriceOffer offer, boolean existing) {
        var reasons = new ArrayList<Reason>(); Eligibility status = Eligibility.ELIGIBLE;
        var product = catalog.products().stream().filter(p -> p.id().equals(plan.productId())).findFirst().orElseThrow();
        if (product.state() != ProductState.ACTIVE || asOf.isBefore(plan.activeFrom()) || (plan.activeUntil() != null && !asOf.isBefore(plan.activeUntil()))
                || asOf.isBefore(offer.validFrom()) || (offer.validUntil() != null && !asOf.isBefore(offer.validUntil()))) {
            reasons.add(reason(Code.NOT_ACTIVE, "Plan/offer/product is not active at calculation time")); status = Eligibility.INELIGIBLE;
        }
        if (plan.accessType() == AccessType.API) { reasons.add(reason(Code.API_NOT_APPLICATION, "API-only billing is not an application subscription")); status = Eligibility.INELIGIBLE; }
        String country = catalog.regions().stream().filter(r -> r.id().equals(offer.regionId())).map(CatalogManifest.Region::countryCode).findFirst().orElse("");
        if (!country.equals(profile.country())) {
            reasons.add(reason(Code.REGION_INELIGIBLE, "Offer is scoped to a different country", offer.regionEvidenceId())); status = Eligibility.INELIGIBLE;
        } else if (!evidence.confirmed(offer.regionEvidenceId())) {
            reasons.add(reason(Code.REGION_UNKNOWN, "Country availability is not confirmed", offer.regionEvidenceId())); status = merge(status, Eligibility.UNKNOWN);
        }
        if (!offer.channel().equals(profile.purchaseChannel())) {
            reasons.add(reason(Code.PURCHASE_CHANNEL_MISMATCH, "Offer channel does not match the requested channel")); status = Eligibility.INELIGIBLE;
        }
        if (!existing && offer.billingPeriod() == BillingPeriod.YEAR && !profile.allowAnnualBilling()) {
            reasons.add(reason(Code.ANNUAL_UPFRONT_REQUIRED, "Annual billing requires opt-in")); status = Eligibility.INELIGIBLE;
        }
        for (var rule : catalog.eligibility().stream().filter(r -> r.offerId().equals(offer.id())).toList()) {
            if (rule.state() == EligibilityState.NOT_REQUIRED && evidence.confirmed(rule.evidenceId())) continue;
            if (rule.kind() == EligibilityKind.STUDENT && rule.state() == EligibilityState.REQUIRED) {
                reasons.add(reason(Code.STUDENT_VERIFICATION_REQUIRED, "Provider student verification is required; a user report is not a Compass audit", rule.evidenceId()));
                status = merge(status, profile.studentStatus() == RecommendationProfile.StudentStatus.NONE ? Eligibility.INELIGIBLE : Eligibility.CONDITIONAL);
            } else {
                reasons.add(reason(Code.ELIGIBILITY_UNKNOWN, "Additional provider condition requires verification: " + rule.requirement(), rule.evidenceId()));
                status = merge(status, Eligibility.UNKNOWN);
            }
        }
        var constraints = new LinkedHashMap<String,Code>();
        profile.requirements().platforms().forEach(p -> constraints.put(coverage.constraint(CapabilityCategory.PLATFORM, p), Code.PLATFORM_UNKNOWN));
        constraints.put(coverage.constraint(CapabilityCategory.INTERFACE_LANGUAGE, profile.interfaceLanguage()), Code.LANGUAGE_UNKNOWN);
        profile.outputLanguages().forEach(l -> constraints.put(coverage.constraint(CapabilityCategory.OUTPUT_LANGUAGE, l), Code.LANGUAGE_UNKNOWN));
        if (profile.requirements().privacy() == RecommendationProfile.Privacy.LOCAL_ONLY)
            constraints.put(coverage.constraint(CapabilityCategory.PRIVACY, "local-only"), Code.PRIVACY_UNKNOWN);
        for (var entry : constraints.entrySet()) {
            var state = coverage.state(plan.productId(), plan.id(), entry.getKey());
            if (state.confirmed() || state.accepted()) continue;
            reasons.add(new Reason(entry.getValue(), "Required constraint: " + entry.getKey(), state.evidenceIds()));
            status = merge(status, state.state() == CapabilityState.UNSUPPORTED ? Eligibility.INELIGIBLE : Eligibility.UNKNOWN);
        }
        for (String id : List.of(offer.priceEvidenceId(), offer.taxEvidenceId(), offer.feeEvidenceId())) {
            String state = evidence.status(id);
            if (state.equals("STALE")) reasons.add(reason(Code.STALE_EVIDENCE, "A monetary claim is stale", id));
            if (state.equals("CONFLICTED")) reasons.add(reason(Code.CONFLICTED_EVIDENCE, "A monetary claim is conflicted", id));
            if (state.equals("CONDITIONAL")) reasons.add(reason(Code.CONDITIONAL_EVIDENCE, "A monetary claim depends on conditions", id));
        }
        return new Assessment(status, List.copyOf(reasons));
    }
    static Eligibility merge(Eligibility a, Eligibility b) { return a.ordinal() >= b.ordinal() ? a : b; }
}

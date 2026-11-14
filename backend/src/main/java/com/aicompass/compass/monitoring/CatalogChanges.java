package com.aicompass.compass.monitoring;

import com.aicompass.compass.catalog.*;
import com.aicompass.compass.recommendation.RecommendationProfile;
import com.aicompass.compass.recommendation.RecommendationResult;
import java.math.BigDecimal;
import java.time.Instant;
import com.aicompass.compass.shared.money.*;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Semantic facts only. No HTML, source fetch, ranking, or source instructions are executed. */
public final class CatalogChanges {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    public record Change(String field, String before, String after) {}

    public List<Change> compare(PublishedCatalogSnapshot before, PublishedCatalogSnapshot after,
            RecommendationProfile profile, Set<String> watchedPlans) {
        return compare(before, after, profile, watchedPlans, after.publishedAsOf());
    }

    public List<Change> compare(PublishedCatalogSnapshot before, PublishedCatalogSnapshot after,
            RecommendationProfile profile, Set<String> watchedPlans, Instant asOf) {
        var plans = new HashSet<>(watchedPlans);
        plans.addAll(relevantPlans(before, profile)); plans.addAll(relevantPlans(after, profile));
        Map<String, String> old = facts(before, profile, plans), next = facts(after, profile, plans);
        var keys = new TreeSet<>(old.keySet()); keys.addAll(next.keySet());
        var changes = new ArrayList<Change>();
        for (String key : keys) {
            String a = old.getOrDefault(key, "UNKNOWN"), b = next.getOrDefault(key, "UNKNOWN");
            if (!a.equals(b)) changes.add(new Change(key, a, b));
        }
        return List.copyOf(changes);
    }

    public boolean significant(List<Change> changes, PublishedCatalogSnapshot before, PublishedCatalogSnapshot after,
            RecommendationResult oldResult, RecommendationResult newResult, Instant asOf) {
        if (changes.isEmpty()) return false;
        // A small price change that changes feasibility/ranking must not hide a budget violation.
        if (!decisions(oldResult).equals(decisions(newResult))) return true;
        Set<String> monetary = Set.of("amount","upfrontAmount","additionalTax","additionalFee");
        if (!changes.stream().allMatch(c -> c.field().startsWith("offer/")
                && monetary.contains(c.field().substring(c.field().lastIndexOf('/')+1)))) return true;
        for (var offer : after.offers()) {
            if (changes.stream().noneMatch(c -> c.field().startsWith("offer/"+offer.id()+"/"))) continue;
            var oldOffer = before.offers().stream().filter(o -> o.id().equals(offer.id())).findFirst().orElse(null);
            if (oldOffer == null) return true;
            var a = OfferCosts.from(oldOffer).allInRecurringMonthly();
            var b = OfferCosts.from(offer).allInRecurringMonthly();
            BigDecimal floor = threshold(after, offer.currency(), asOf);
            if (a.isEmpty() || b.isEmpty() || !oldOffer.currency().equals(offer.currency()) || floor == null) return true;
            BigDecimal previous = a.get().amount();
            BigDecimal delta = b.get().amount().subtract(previous).abs();
            if (delta.compareTo(previous.multiply(new BigDecimal("0.10")).max(floor)) >= 0) return true;
        }
        return false;
    }

    private List<String> decisions(RecommendationResult result) {
        var decisions = new ArrayList<String>();
        decisions.add(result.status()+":"+result.searchComplete());
        java.util.stream.Stream.concat(result.bundles().stream(),result.conditionalAlternatives().stream())
                .forEach(b -> decisions.add(b.id()+":"+b.strategies()+":"+b.eligibility()+":"+b.costs().confirmedBudgetFit()));
        return decisions;
    }

    private BigDecimal threshold(PublishedCatalogSnapshot s, String currency, Instant asOf) {
        if (currency.equals("USD")) return new BigDecimal("2");
        for (var fx : s.fx()) {
            if (!fx.baseCurrency().equals("USD") || !fx.quoteCurrency().equals(currency)) continue;
            boolean verified = s.evidence().stream().anyMatch(e -> e.id().equals(fx.evidenceId())
                    && (e.status().equals("VERIFIED_SOURCE") || s.datasetKind().equals("DEMO") && e.status().equals("DEMO"))
                    && e.verifiedAt() != null && !e.verifiedAt().isAfter(asOf)
                    && asOf.isBefore(e.verifiedAt().plusSeconds(86400)));
            if (!verified) continue;
            try {
                return FxConversion.convert(Money.of("2","USD"), new FxConversion.Snapshot(fx.id(),
                        Currency.getInstance("USD"),Currency.getInstance(currency),new BigDecimal(fx.rate()),fx.observedAt()),
                        asOf, java.math.RoundingMode.CEILING).amount();
            } catch (IllegalArgumentException unavailable) { /* Missing/future/stale FX remains unknown. */ }
        }
        return null;
    }

    private Set<String> relevantPlans(PublishedCatalogSnapshot s, RecommendationProfile p) {
        var capabilities = new HashSet<String>();
        p.requirements().features().forEach(f -> capabilities.add(f.capabilityId()));
        s.mappings().stream().filter(m -> p.needs().stream().anyMatch(n -> n.id().equals(m.needId())))
                .forEach(m -> capabilities.add(m.capabilityId()));
        var plans = new HashSet<String>();
        s.assertions().stream().filter(a -> capabilities.contains(a.capabilityId()) && (a.state() == CatalogTypes.CapabilityState.SUPPORTED || a.state() == CatalogTypes.CapabilityState.PARTIAL)).forEach(a -> {
            if (a.planId() != null) plans.add(a.planId());
            if (a.productId() != null) s.plans().stream().filter(t -> t.productId().equals(a.productId())).forEach(t -> plans.add(t.id()));
        });
        return plans;
    }

    private Map<String,String> facts(PublishedCatalogSnapshot s, RecommendationProfile p, Set<String> plans) {
        Map<String,String> facts = new TreeMap<>();
        Set<String> evidence = new HashSet<>(), offers = new HashSet<>(), products = new HashSet<>();
        s.plans().stream().filter(t -> plans.contains(t.id())).forEach(t -> { products.add(t.productId()); add(facts,evidence,"plan/"+t.id(),t); });
        s.products().stream().filter(t -> products.contains(t.id())).forEach(t -> add(facts,evidence,"product/"+t.id(),t));
        s.offers().stream().filter(o -> plans.contains(o.planId()) && country(s,o.regionId()).equals(p.country()) && o.channel().equals(p.purchaseChannel()))
                .forEach(o -> { offers.add(o.id()); add(facts,evidence,"offer/"+o.id(),o); });
        s.assertions().stream().filter(a -> plans.contains(a.planId()) || products.contains(a.productId())).forEach(a -> add(facts,evidence,"capability/"+a.id(),a));
        s.agentAccess().stream().filter(a -> plans.contains(a.planId())).forEach(a -> add(facts,evidence,"agent/"+a.id(),a));
        s.modelAccess().stream().filter(a -> plans.contains(a.planId())).forEach(a -> add(facts,evidence,"model/"+a.id(),a));
        s.pools().stream().filter(a -> offers.contains(a.offerId())).forEach(a -> add(facts,evidence,"quota/"+a.id(),a));
        s.limits().stream().filter(a -> offers.contains(a.offerId())).forEach(a -> add(facts,evidence,"limit/"+a.id(),a));
        s.eligibility().stream().filter(a -> offers.contains(a.offerId())).forEach(a -> add(facts,evidence,"condition/"+a.id(),a));
        s.evidence().stream().filter(e -> evidence.contains(e.id())).forEach(e -> {
            facts.put("evidence/"+e.id()+"/status", e.effectiveStatus());
            facts.put("evidence/"+e.id()+"/conditions", e.conditions());
        });
        return facts;
    }

    private String country(PublishedCatalogSnapshot s, String region) {
        return s.regions().stream().filter(r -> r.id().equals(region)).map(CatalogManifest.Region::countryCode).findFirst().orElse("UNKNOWN");
    }
    private void add(Map<String,String> facts, Set<String> evidence, String key, Object value) {
        JsonNode node = JSON.valueToTree(value);
        node.properties().forEach(e -> {
            String name = e.getKey();
            if (name.toLowerCase(Locale.ROOT).contains("evidenceid")) { if (!e.getValue().isNull()) evidence.add(e.getValue().asString()); return; }
            if (Set.of("id","name","slug","tier","description","websiteUrl","metadataEvidenceId").contains(name)) return;
            facts.put(key+"/"+name, e.getValue().isNull() ? "UNKNOWN" : e.getValue().isString() ? e.getValue().asString() : e.getValue().toString());
        });
    }
}

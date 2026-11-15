package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.aicompass.compass.recommendation.EngineFixtures.*;
import static com.aicompass.compass.catalog.CatalogTypes.*;

class CatalogExpansionTest {
    static final Instant CHECKED = Instant.parse("2026-11-14T23:43:01Z");
    static CatalogManifest manifest(String file) throws Exception {
        try (var in = CatalogExpansionTest.class.getResourceAsStream("/data/catalog/" + file)) {
            return new CatalogJson().read(new String(Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    static CatalogManifest expanded() throws Exception { return manifest("catalog-2026-11-14-ce1.json"); }
    static RecommendationProfile profile(String country, String currency, String... needs) {
        return change(student(), t -> {
            t.put("country", country).put("interfaceLanguage", "en").put("asOf", CHECKED.toString());
            t.putArray("outputLanguages").add("en");
            ((ObjectNode)t.get("requirements")).putArray("platforms").add("WEB");
            ((ObjectNode)t.get("budget")).put("currency", currency).put("monthlyMax", "100").put("upfrontMax", "300");
            var n = t.putArray("needs");
            for (String id : needs) n.addObject().put("id", id).put("weight", 5).put("required", true).put("intensity", "WEEKLY");
        });
    }
    static RecommendationResult calculate(RecommendationProfile p) throws Exception {
        return new RecommendationEngine().calculate(p, snapshot(expanded()), CHECKED);
    }
    @Test void reviewedManifestValidatesAndPreservesEveryHistoricalRecord() throws Exception {
        var m = expanded();
        try (var f = Validation.buildDefaultValidatorFactory()) {
            new CatalogImportValidator(f.getValidator(), Clock.fixed(CHECKED, ZoneOffset.UTC)).validate(m, false);
        }
        var before = JSON.valueToTree(manifest("catalog-2026-11-14-r2.json"));
        var after = JSON.valueToTree(m);
        for (var entry : before.properties()) if (entry.getValue().isArray()) {
            for (var record : entry.getValue()) {
                assertTrue(after.get(entry.getKey()).valueStream().anyMatch(record::equals), entry.getKey() + ": " + record.get("id"));
            }
        }
        assertEquals(23, m.products().size());
        assertEquals(26, m.subscriptionPlans().size());
        assertTrue(m.evidenceSources().stream().noneMatch(s -> s.kind() == SourceKind.SYNTHETIC));
        assertTrue(m.benchmarkObservations().isEmpty()); assertTrue(m.fxSnapshots().isEmpty());
    }
    @Test void renewedFreeQuotasAreDistinctFromSignupCreditsAndUnknownPrices() throws Exception {
        var m = expanded();
        assertTrue(m.priceOffers().stream().filter(o -> o.priceKind() == PriceKind.UNKNOWN).allMatch(o -> o.amount() == null && o.upfrontAmount() == null));
        assertTrue(m.quotaPools().stream().filter(q -> q.offerId().startsWith("higgsfield-free")).allMatch(q -> "0".equals(q.quantity()) && q.resetPeriod() == ResetPeriod.MONTH));
        assertTrue(m.quotaPools().stream().filter(q -> q.offerId().startsWith("runway-free")).allMatch(q -> q.resetPeriod() == ResetPeriod.UNKNOWN && q.quantity() == null));
        assertTrue(m.quotaPools().stream().filter(q -> q.offerId().startsWith("descript-free")).allMatch(q -> "60".equals(q.quantity()) && q.unit().equals("media minutes")));
        for (String p : List.of("leonardo", "suno")) assertTrue(m.quotaPools().stream().filter(q -> q.offerId().startsWith(p + "-free")).allMatch(q -> q.resetPeriod() == ResetPeriod.DAY));
    }
    @Test void annualFullChargeAndNoKzConversionAreExplicit() throws Exception {
        var m = expanded();
        var annual = m.priceOffers().stream().filter(o -> o.id().equals("midjourney-basic-annual-us-year")).findFirst().orElseThrow();
        assertEquals("96", annual.amount()); assertEquals("96", annual.upfrontAmount()); assertEquals(BillingPeriod.YEAR, annual.billingPeriod());
        assertTrue(m.priceOffers().stream().filter(o -> o.id().startsWith("kimi-plus-kz")).allMatch(o -> o.priceKind() == PriceKind.UNKNOWN && o.amount() == null && o.currency().equals("KZT")));
    }
    @Test void annualAndMonthlyBillingAreOffersOfOneRealTier() throws Exception {
        var offers = expanded().priceOffers().stream().filter(o -> o.id().startsWith("midjourney-basic") && o.regionId().equals("us")).toList();
        assertEquals(2, offers.size());
        assertEquals(1, offers.stream().map(CatalogManifest.PriceOffer::planId).distinct().count());
    }
    @Test void includedAgentsAndSharedCreditsAreNotSeparatePurchases() throws Exception {
        var m = expanded();
        assertEquals(4, m.agents().size());
        assertTrue(m.products().stream().noneMatch(p -> p.id().equals("kimi-coding-agent")));
        var limits = m.usageLimits().stream().filter(l -> l.offerId().equals("kimi-plus-cn-month")).toList();
        assertEquals(4, limits.size()); assertEquals(1, limits.stream().map(CatalogManifest.UsageLimit::poolId).distinct().count());
        assertEquals(2, m.planModelAccess().stream().filter(a -> a.planId().equals("runway-standard")).count());
    }
    @Test void threeRealProfilesHaveDifferentEvidenceBasedConditionalCoverage() throws Exception {
        var student = calculate(profile("US", "USD", "study", "development", "presentations"));
        var creator = calculate(profile("US", "USD", "images", "video", "editing", "voice"));
        var researcher = calculate(profile("US", "USD", "research"));
        for (var r : List.of(student, creator, researcher)) {
            assertEquals("CONDITIONAL_ONLY", r.status());
            assertFalse(r.conditionalAlternatives().isEmpty()); assertTrue(r.bundles().isEmpty());
            assertTrue(r.searchComplete());
            assertTrue(r.conditionalAlternatives().stream().allMatch(b -> b.items().size() <= 4 && !b.costs().confirmedBudgetFit()));
        }
        assertNotEquals(ids(student.conditionalAlternatives().getFirst()), ids(creator.conditionalAlternatives().getFirst()));
        assertTrue(student.conditionalAlternatives().stream().anyMatch(b -> b.items().stream().anyMatch(i -> i.productId().equals("canva"))));
        System.out.println("CE real profiles: student=" + ids(student.conditionalAlternatives().getFirst()) + " creator=" + ids(creator.conditionalAlternatives().getFirst()) + " researcher=" + ids(researcher.conditionalAlternatives().getFirst()));
    }
    @Test void zeroBudgetDoesNotInventKnownCheckoutAndMissingNeedHasNoMatch() throws Exception {
        var free = change(profile("US", "USD", "study", "development", "presentations"), t -> ((ObjectNode)t.get("budget")).put("monthlyMax", "0").put("upfrontMax", "0"));
        var r = calculate(free); assertTrue(r.bundles().isEmpty());
        assertFalse(r.conditionalAlternatives().isEmpty());
        assertTrue(r.conditionalAlternatives().stream().anyMatch(b -> b.items().stream().allMatch(i -> i.planId().endsWith("free"))));
        var absent = calculate(profile("KZ", "USD", "not-in-catalog"));
        assertEquals("NO_MATCH", absent.status()); assertTrue(absent.conditionalAlternatives().isEmpty());
    }
    @Test void chinaPriceWithoutFxAndKzWithoutCheckoutRemainUnknown() throws Exception {
        var cn = calculate(profile("CN", "USD", "agents"));
        assertTrue(cn.bundles().isEmpty()); assertTrue(cn.fxSnapshots().isEmpty());
        // Missing FX must not convert the listed CNY charge into USD or a known total.
        assertFalse(cn.conditionalAlternatives().isEmpty());
        assertTrue(cn.conditionalAlternatives().stream().allMatch(b -> b.costs().recurringMonthly() == null));
        assertTrue(cn.conditionalAlternatives().stream().flatMap(b -> b.items().stream()).anyMatch(i -> i.originalCurrency().equals("CNY") && "99".equals(i.originalCharge())));
        var kz = calculate(profile("KZ", "KZT", "study", "presentations"));
        assertTrue(kz.bundles().isEmpty());
        assertTrue(kz.conditionalAlternatives().stream().allMatch(b -> b.costs().recurringMonthly() == null));
    }
    @Test void listedPaidBaseCannotFitZeroBudgetEvenWhenCheckoutIsUnknown() throws Exception {
        var p = change(profile("US", "USD", "images", "video", "editing"), t -> ((ObjectNode)t.get("budget")).put("monthlyMax", "0").put("upfrontMax", "0"));
        var m = expanded();
        var byId = new java.util.HashMap<String, CatalogManifest.PriceOffer>();
        m.priceOffers().forEach(o -> byId.put(o.id(), o));
        var result = calculate(p);
        for (var b : result.conditionalAlternatives()) for (var item : b.items()) {
            var o = byId.get(item.offerId());
            assertNotEquals(PriceKind.FIXED, o.priceKind(), "Known positive base cannot fit zero budget: " + o.id());
        }
    }
    @Test void realCatalogPerformanceRemainsBoundedAndDeterministic() throws Exception {
        var p = profile("US", "USD", "images", "video", "editing", "voice");
        var c = snapshot(expanded()); var engine = new RecommendationEngine();
        var expected = engine.calculate(p, c, CHECKED);
        long[] times = new long[10];
        for (int i = 0; i < times.length; i++) {
            long start = System.nanoTime(); assertEquals(expected, engine.calculate(p, c, CHECKED)); times[i] = System.nanoTime() - start;
        }
        Arrays.sort(times);
        String report = "CE real catalog p95_ms=" + times[9] / 1_000_000.0 + " nodes=" + expected.search().visitedCombinations();
        System.out.println(report); assertTrue(times[9] < 1_000_000_000L, report);
    }
}

package com.aicompass.compass.recommendation;

import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;
import static com.aicompass.compass.recommendation.EngineFixtures.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;

class ProfileAndInvariantTest {
    @Test void validTypedProfileAndDecimalStringsRoundTrip() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=new ProfileValidator(factory.getValidator());
            assertEquals(student(),validator.read(JSON.writeValueAsString(student()),NOW));
        }
    }
    @ParameterizedTest @ValueSource(strings={"duplicate","negative","numeric-money","currency","maxTools","weight","extra","null","missing","duplicate-key","future","date","large","numeric-time","null-language","fractional-weight","date-array"})
    void rejectsInvalidProfiles(String kind) {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=new ProfileValidator(factory.getValidator());var p=(ObjectNode)JSON.valueToTree(student());
            switch(kind) {
                case "duplicate" -> ((tools.jackson.databind.node.ArrayNode)p.get("needs")).add(p.get("needs").get(0));
                case "negative" -> ((ObjectNode)p.get("budget")).put("monthlyMax","-1");
                case "numeric-money" -> ((ObjectNode)p.get("budget")).put("monthlyMax",20);
                case "currency" -> ((ObjectNode)p.get("budget")).put("currency","XXX");
                case "maxTools" -> p.put("maxTools",5);
                case "fractional-weight" -> ((ObjectNode)p.get("needs").get(0)).put("weight",2.5);
                case "weight" -> ((ObjectNode)p.get("needs").get(0)).put("weight",0);
                case "extra" -> p.put("premium",true);
                case "null-language" -> p.putArray("outputLanguages").addNull();
                case "null" -> p.putNull("requirements");
                case "missing" -> p.remove("country");
                case "future" -> p.put("asOf",NOW.plusSeconds(1).toString());
                case "numeric-time" -> p.put("asOf",NOW.getEpochSecond());
                case "date-array" -> {
                    p=(ObjectNode)JSON.valueToTree(RecommendationEngineTest.existing(student(),"learner-plan","learner-offer","0",true,"MONTH","2026-12-01"));
                    ((ObjectNode)p.get("existingSubscriptions").get(0)).putArray("nextRenewal").add(2026).add(12).add(1);
                }
                case "date" -> p.put("asOf","not-a-date");
                default -> {}
            }
            String json=JSON.writeValueAsString(p);
            if(kind.equals("duplicate-key"))json=json.replaceFirst("\\{","{\"maxTools\":2,");
            if(kind.equals("large"))json=" ".repeat(65537)+json;
            final String input=json;assertThrows(org.springframework.web.server.ResponseStatusException.class,()->validator.read(input,NOW));
        }
    }
    @Test void seededGeneratedProfilesNeverBreakBudgetRequiredFeaturesOrUniqueness() {
        var rng=new Random(4042026L);var catalog=snapshot();var engine=new RecommendationEngine();
        for(int i=0;i<160;i++) {
            int budget=rng.nextInt(25),upfront=rng.nextInt(150),max=rng.nextInt(4)+1;
            boolean annual=rng.nextBoolean();
            var p=change(i%2==0?student():editor(),t -> {t.put("maxTools",max).put("allowAnnualBilling",annual);
                ((ObjectNode)t.get("budget")).put("monthlyMax",String.valueOf(budget)).put("upfrontMax",String.valueOf(upfront));});
            var r=engine.calculate(p,catalog,NOW);
            assertEquals(r,engine.calculate(p,catalog,NOW));
            assertEquals(r.bundles().size(),r.bundles().stream().map(Bundle::id).distinct().count());
            for(var b:r.bundles()) {
                assertTrue(new BigDecimal(b.costs().recurringMonthly()).compareTo(BigDecimal.valueOf(budget))<=0);
                assertTrue(new BigDecimal(b.costs().cashDueNow()).compareTo(BigDecimal.valueOf(upfront))<=0);
                assertTrue(b.items().size()<=max);
                assertEquals(b.items().size(),b.items().stream().map(Item::productId).distinct().count());
                assertTrue(b.coverage().stream().filter(Coverage::required).allMatch(c->c.confirmed()||c.acceptedForRequirement()));
                assertTrue(b.items().stream().allMatch(x->x.billingPeriod().equals("MONTH")||annual));
                assertTrue(b.items().stream().noneMatch(x->x.productId().equals("student")));
            }
        }
    }
    @Test void staleAndConflictedSupportAreUnknownInsteadOfUnsupported() {
        for(String status:List.of("STALE","CONFLICTED","UNKNOWN")) {
            var c=snapshot(data(t -> {for(var a:t.get("capabilityAssertions"))if(a.get("capabilityId").asString().equals("study")) {
                row(t,"evidence",a.get("evidenceId").asString()).put("status",status);
            }}));
            var r=new RecommendationEngine().calculate(student(),c,NOW);assertTrue(r.bundles().isEmpty());
            assertTrue(r.unmetRequirements().stream().anyMatch(x->x.capabilityId().equals("study")&&x.state()==com.aicompass.compass.catalog.CatalogTypes.CapabilityState.UNKNOWN));
        }
    }
    @Test void limiterUsesBoundedWindowAndRecoversWithoutForwardedHeaders() {
        class MutableClock extends Clock {Instant time=NOW;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return time;}}
        var clock=new MutableClock();var guard=new PreviewLoadGuard(clock,JSON);
        for(int i=0;i<30;i++)assertTrue(guard.allow("127.0.0.1"));
        assertFalse(guard.allow("127.0.0.1"));clock.time=NOW.plusSeconds(60);assertTrue(guard.allow("127.0.0.1"));
    }
}

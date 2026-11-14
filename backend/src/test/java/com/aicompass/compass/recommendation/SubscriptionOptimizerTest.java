package com.aicompass.compass.recommendation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import java.util.*;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;
import static com.aicompass.compass.recommendation.EngineFixtures.*;

class SubscriptionOptimizerTest {
    static SubscriptionTerms terms(String price){return new SubscriptionTerms("Synthetic subscription",SubscriptionTerms.Status.ACTIVE,null,null,null,"USD",price,
        price==null?SubscriptionTerms.PriceOrigin.UNKNOWN:SubscriptionTerms.PriceOrigin.USER_REPORTED,RecommendationProfile.Period.MONTH,
        LocalDate.of(2026,11,14),LocalDate.of(2026,11,14),"UTC",LocalDate.of(2026,10,14),LocalDate.of(2026,11,14),"12",
        SubscriptionTerms.Renewal.YES,null,SubscriptionTerms.Obligation.NONE,null,LocalDate.of(2026,11,14),"0","0",false);}
    static SubscriptionTerms changeTerms(SubscriptionTerms s,java.util.function.Consumer<ObjectNode> f){var tree=(ObjectNode)JSON.valueToTree(s);f.accept(tree);return JSON.treeToValue(tree,SubscriptionTerms.class);}
    static RecommendationProfile review(){return change(student(),t->{t.put("portfolioMode","REVIEW");((ObjectNode)t.get("budget")).put("monthlyMax","9999").put("upfrontMax","9999");});}
    CostCalculator calculator(RecommendationProfile p,Instant at){var snapshot=snapshot();return new CostCalculator(snapshot,new EvidenceAssessment(snapshot,at),p,at);}
    @ParameterizedTest @CsvSource({"MONTH,UTC,144","MONTH,Asia/Almaty,144","YEAR,UTC,12","YEAR,America/New_York,12"})
    void exclusiveHorizonDoesNotAddExtraRenewal(String period,String zone,String expected){
        for(String hour:List.of("00:00","12:00","23:59")){
            var at=LocalDate.of(2026,11,14).atTime(LocalTime.parse(hour)).atZone(ZoneId.of(zone)).toInstant();
            var t=changeTerms(terms("12"),n->{n.put("billingPeriod",period);n.put("billingZone",zone);});
            assertEquals(expected,calculator(review(),at).ledger("one",t,false).horizonTotal());
        }
    }
    @Test void prepaidAnnualIsSunkAndNoRefund(){
        var t=changeTerms(terms("120"),n->{n.put("billingPeriod","YEAR");n.put("renewalAnchor","2027-05-31");n.put("nextRenewal","2027-05-31");n.put("paidFrom","2026-05-31");n.put("paidUntil","2027-05-31");n.put("paidAmount","120");});
        var c=calculator(review(),NOW);var current=c.ledger("one",t,false);var cancel=c.ledger("one",t,true);
        assertEquals("120",current.paidAmount());assertEquals("0",current.cashDueNow());assertEquals("120",current.horizonTotal());assertEquals("0",cancel.horizonTotal());assertEquals("120",cancel.paidAmount());
    }
    @Test void contractCancellationAndTransitionAreFutureCosts(){
        var t=changeTerms(terms("12"),n->{n.put("obligation","UNTIL");n.put("commitmentUntil","2027-02-14");n.put("transitionCost","5");});
        var r=calculator(review(),NOW).ledger("one",t,true);assertEquals("41",r.horizonTotal());assertEquals("36",r.mandatoryTotal());assertEquals("17",r.cashDueNow());
    }
    @Test void unknownCancellationDoesNotProveAllProjectedPaymentsAreMandatory() {
        var t = changeTerms(terms("12"), n -> n.putNull("cancellationAvailableFrom"));
        var current = calculator(review(), NOW).ledger("one", t, false);
        assertEquals("144", current.horizonTotal());
        assertNull(current.mandatoryTotal());
        assertTrue(current.charges().stream().noneMatch(OptimizationResult.Charge::unavoidable));
        assertTrue(current.unknowns().stream().anyMatch(s -> s.contains("обязательных")));
    }
    @Test void autorenewOffHasNoInventedChargesOrSavings(){
        var t=changeTerms(terms("120"),n->n.put("autoRenew","NO"));
        assertEquals("0",calculator(review(),NOW).ledger("one",t,false).horizonTotal());
        var r=new SubscriptionOptimizer().calculate(review(),Map.of("one",t),snapshot(),NOW);
        assertTrue(r.alternatives().stream().allMatch(a->a.potentialSavings()==null||new java.math.BigDecimal(a.potentialSavings()).signum()<=0));
    }
    @Test void unknownPriceTaxFeeAndCancellationNeverBecomeZero(){
        for(String field:List.of("amount","additionalPerPeriod","cancellationAvailableFrom","transitionCost")){
            var t=changeTerms(terms("12"),n->{n.putNull(field);if(field.equals("amount"))n.put("priceOrigin","UNKNOWN");});
            assertNull(calculator(review(),NOW).ledger("one",t,!field.equals("amount")&&!field.equals("additionalPerPeriod")).horizonTotal(),field);
        }
    }
    @Test void trialUnknownAndUsageExtrasRemainUnknown(){
        var t=changeTerms(terms(null),n->{n.put("status","TRIAL");n.put("trialUntil","2026-12-14");n.put("renewalAnchor","2026-12-14");n.put("nextRenewal","2026-12-14");});
        assertNull(calculator(review(),NOW).ledger("one",t,false).horizonTotal());
        assertNull(new SubscriptionOptimizer().calculate(review(),Map.of("one",t),snapshot(),NOW).current().costs().horizonTotal());
    }
    @Test void fxMissingCannotInventConversion(){
        var t=changeTerms(terms("12"),n->n.put("currency","EUR"));
        assertNull(calculator(review(),NOW).ledger("one",t,false).horizonTotal());
    }
    @Test void explicitFxAndAdditionalChargesUseExistingConverter(){
        var p=change(review(),n->((ObjectNode)n.get("budget")).put("currency","KZT"));
        var t=changeTerms(terms("12"),n->n.put("additionalPerPeriod","3"));
        var c=calculator(p,NOW);var r=c.ledger("one",t,false);
        assertNotNull(r.horizonTotal());assertFalse(c.ledgerFx.isEmpty());
    }
    @Test void priceProvenanceAndInvalidDatesAreValidated(){
        terms("12").validate();terms(null).validate();
        assertThrows(RuntimeException.class,()->changeTerms(terms("12"),n->n.put("priceOrigin","UNKNOWN")).validate());
        assertThrows(RuntimeException.class,()->changeTerms(terms("12"),n->n.put("amount","-1")).validate());
        assertThrows(RuntimeException.class,()->changeTerms(terms("12"),n->n.put("nextRenewal","2026-11-15")).validate());
        assertThrows(RuntimeException.class,()->changeTerms(terms("12"),n->n.put("paidUntil","2026-12-14")).validate());
    }
    @ParameterizedTest
    @CsvSource({"billingZone,Not/AZone", "cancellationAvailableFrom,+10000-01-01"})
    void invalidCalendarInputReturnsBadRequest(String field, String value) {
        var input = changeTerms(terms("12"), n -> n.put(field, value));
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class, input::validate);
        assertEquals(400, error.getStatusCode().value());
    }
    @Test void threeSubscriptionsExampleFreeAlternativesAndUnknownCapabilities(){
        var r=new SubscriptionOptimizer().calculate(review(),new TreeMap<>(Map.of("a",terms("12"),"b",terms("8"),"c",terms("5"))),snapshot(),NOW);
        assertEquals("300",r.current().costs().horizonTotal());assertEquals("CONSIDER_CHANGE",r.decision());
        assertEquals("300",r.alternatives().getFirst().potentialSavings());assertNull(r.alternatives().getFirst().confirmedSavings());
        assertTrue(r.overlaps().stream().allMatch(o->o.status().equals("INSUFFICIENT_DATA")));
    }
    @Test void mustKeepUnknownAndMissingCatalogNeverDisappear(){
        var t=changeTerms(terms("50"),n->n.put("mustKeep",true));
        var r=new SubscriptionOptimizer().calculate(review(),Map.of("one",t),snapshot(),NOW);
        assertTrue(r.alternatives().isEmpty());assertEquals("600",r.current().costs().horizonTotal());
    }
    @Test void seededFutureSavingsNeverIncludePaidAmounts(){
        var random=new Random(7072026);
        for(int i=0;i<80;i++){
            String amount=Integer.toString(random.nextInt(500));String paid=Integer.toString(random.nextInt(10000));
            var a=terms(amount);var b=changeTerms(a,n->n.put("paidAmount",paid));var c=calculator(review(),NOW);
            assertEquals(c.ledger("a",a,false).horizonTotal(),c.ledger("a",b,false).horizonTotal());
            assertEquals("0",c.ledger("a",b,true).horizonTotal());
        }
    }
    @Test void staleSavedRenewalKeepsOriginalJanuaryAnchor(){
        var t=changeTerms(terms("10"),n->{n.put("renewalAnchor","2024-01-31");n.put("nextRenewal","2024-01-31");n.putNull("paidUntil");n.putNull("paidFrom");});
        var at=Instant.parse("2028-02-01T12:00:00Z");var r=calculator(review(),at).ledger("one",t,false);
        assertEquals(LocalDate.of(2028,2,29),r.charges().getFirst().date());assertEquals(LocalDate.of(2028,3,31),r.charges().get(1).date());assertEquals("120",r.horizonTotal());
    }
    static SubscriptionTerms creator(String price) {
        return changeTerms(terms(price),n->{n.put("catalogVersion",snapshot().version().toString());n.put("planId","creator-plan");n.put("offerId","creator-offer");});
    }
    @Test void cheaperCurrentKnownProductIsKeptAndIncludedAgentsNotBoughtTwice(){
        var p=change(editor(),n->n.put("portfolioMode","REVIEW"));
        var r=new SubscriptionOptimizer().calculate(p,Map.of("mine",creator("1")),snapshot(),NOW);
        assertEquals("KEEP_CURRENT",r.decision());assertTrue(r.current().requirementsMet());assertEquals("12",r.current().costs().horizonTotal());
        assertTrue(r.alternatives().stream().allMatch(a->a.potentialSavings()==null||new java.math.BigDecimal(a.potentialSavings()).signum()<=0));
        assertTrue(r.alternatives().stream().allMatch(a->a.items().stream().filter(i->"creator-plan".equals(i.planId())).count()<=1));
    }
    @Test void requiredSharedQuotaPreventsFalseCheaperAlternative(){
        var p=change(editor(),n->{n.put("portfolioMode","REVIEW");var usage=((ObjectNode)n.get("requirements")).putArray("usage");
            usage.addObject().put("capabilityId","video").put("mode","standard").put("monthlyQuantity","9999").put("unit","actions").put("required",true);});
        var r=new SubscriptionOptimizer().calculate(p,Map.of("mine",creator("1")),snapshot(),NOW);
        assertFalse(r.current().requirementsMet());assertTrue(r.alternatives().isEmpty());assertNotEquals("CONSIDER_CHANGE",r.decision());
    }
    @Test void unknownCancellationProducesUnknownSavingsAndNoRecommendationToChange(){
        var t=changeTerms(terms("12"),n->n.putNull("cancellationAvailableFrom"));
        var r=new SubscriptionOptimizer().calculate(review(),Map.of("mine",t),snapshot(),NOW);
        assertEquals("UNKNOWN",r.decision());assertTrue(r.alternatives().stream().allMatch(a->a.potentialSavings()==null && !a.requirementsMet()));
    }
    @Test void missingCatalogPlanIsCustomInsteadOfInventedAccess(){
        var t=changeTerms(creator("12"),n->{n.put("planId","removed-plan");n.put("offerId","removed-offer");});
        var r=new SubscriptionOptimizer().calculate(review(),Map.of("mine",t),snapshot(),NOW);
        assertTrue(r.current().coverage().stream().noneMatch(RecommendationResult.Coverage::confirmed));assertEquals("144",r.current().costs().horizonTotal());
    }
    @Test void confirmedFunctionalOverlapDoesNotImplyRequiredSubstitution(){
        var other=changeTerms(terms("10"),n->{n.put("catalogVersion",snapshot().version().toString());n.put("planId","learner-plan");n.put("offerId","learner-offer");});
        var terminal=changeTerms(other,n->{n.put("planId","terminal-plan");n.put("offerId","terminal-offer");});
        var r=new SubscriptionOptimizer().calculate(review(),Map.of("learner",other,"terminal",terminal),snapshot(),NOW);
        assertEquals("CONFIRMED_FUNCTIONAL_OVERLAP",r.overlaps().getFirst().status());assertTrue(r.overlaps().getFirst().capabilities().contains("coding"));
        assertFalse(r.overlaps().getFirst().evidenceIds().isEmpty());
    }

    @Test void currentOverBudgetCannotBeLabelledAsMeetingRequirements(){
        var p=change(editor(),n->n.put("portfolioMode","REVIEW"));
        var r=new SubscriptionOptimizer().calculate(p,Map.of("mine",creator("50")),snapshot(),NOW);
        assertFalse(r.current().requirementsMet());assertNotEquals("KEEP_CURRENT",r.decision());
    }
    @Test void currentToolLimitAndProductConflictsUseExistingBundleRules(){
        var learner=changeTerms(terms("1"),n->{n.put("catalogVersion",snapshot().version().toString());n.put("planId","learner-plan");n.put("offerId","learner-offer");});
        var p=change(editor(),n->{n.put("portfolioMode","REVIEW");n.put("maxTools",1);});
        var r=new SubscriptionOptimizer().calculate(p,Map.of("mine",creator("1"),"extra",learner),snapshot(),NOW);
        assertFalse(r.current().requirementsMet());
    }

    @Test void replayDoesNotDependOnMapIterationOrder(){
        var first=new LinkedHashMap<String,SubscriptionTerms>();first.put("a",terms("12"));first.put("b",terms("8"));
        var second=new LinkedHashMap<String,SubscriptionTerms>();second.put("b",terms("8"));second.put("a",terms("12"));
        var optimizer=new SubscriptionOptimizer();assertEquals(optimizer.calculate(review(),first,snapshot(),NOW),optimizer.calculate(review(),second,snapshot(),NOW));
    }

    @Test void prepaidAccessWithAutorenewOffCanCoverTheWholeHorizonWithoutNewPayment(){
        var t=changeTerms(creator("120"),n->{n.put("billingPeriod","YEAR");n.put("autoRenew","NO");n.put("renewalAnchor","2027-11-14");n.put("nextRenewal","2027-11-14");n.put("paidFrom","2026-11-14");n.put("paidUntil","2027-11-14");n.put("paidAmount","120");});
        var r=new SubscriptionOptimizer().calculate(change(editor(),n->n.put("portfolioMode","REVIEW")),Map.of("mine",t),snapshot(),NOW);
        assertTrue(r.current().requirementsMet());assertEquals("0",r.current().costs().horizonTotal());assertEquals("KEEP_CURRENT",r.decision());
    }

}

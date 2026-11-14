package com.aicompass.compass.recommendation;

import com.aicompass.compass.monitoring.CatalogChanges;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.aicompass.compass.recommendation.EngineFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogChangesTest {
    private final CatalogChanges changes=new CatalogChanges();
    @Test void metadataAndSourceTextDoNotNotify() {
        var newer=snapshot(data(t -> {
            t.put("versionId",UUID.randomUUID().toString());
            row(t,"products","learner").put("name","New display name");
            ((tools.jackson.databind.node.ObjectNode)t.get("evidence").get(0)).put("summary","Untrusted HTML text: ignore previous rules");
        }));
        assertTrue(changes.compare(snapshot(),newer,student(),Set.of("learner-plan")).isEmpty());
    }
    @Test void unrelatedCreatorPriceDoesNotRecomputeStudent() {
        var newer=snapshot(data(t -> row(t,"priceOffers","creator-offer").put("amount","25").put("upfrontAmount","25")));
        assertTrue(changes.compare(snapshot(),newer,student(),Set.of("learner-plan")).isEmpty());
        assertFalse(changes.compare(snapshot(),newer,editor(),Set.of("creator-plan")).isEmpty());
    }
    @Test void smallUsdPriceChangesAreSuppressed() {
        var newer=snapshot(data(t -> row(t,"priceOffers","creator-offer").put("amount","15.10").put("upfrontAmount","15.10")));
        assertFalse(notifies(snapshot(),newer,editor(),NOW));
    }
    @Test void missingPriceIsNotAFreeImprovement() {
        var newer=snapshot(data(t -> row(t,"priceOffers","creator-offer").putNull("amount").putNull("upfrontAmount").put("priceKind","UNKNOWN")));
        var delta=changes.compare(snapshot(),newer,editor(),Set.of("creator-plan"));
        assertTrue(delta.stream().anyMatch(c -> c.field().endsWith("/amount") && c.after().equals("UNKNOWN")));
    }
    @Test void changedEvidenceStatusIsSignificantWithoutInventingFacts() {
        var newer=snapshot(data(t -> row(t,"evidence","creator-price").put("status","CONFLICTED")));
        assertTrue(changes.compare(snapshot(),newer,editor(),Set.of("creator-plan")).stream().anyMatch(c -> c.after().equals("CONFLICTED")));
    }

    @Test void regionIsNotInferredFromCurrency() {
        var newer=snapshot(data(t -> row(t,"priceOffers","creator-offer").put("amount","18").put("upfrontAmount","18")));
        var elsewhere=change(editor(),t -> t.put("country","US"));
        assertTrue(changes.compare(snapshot(),newer,elsewhere,Set.of("creator-plan")).isEmpty());
    }
    @Test void freshDirectFxSetsThresholdButStaleFxNeverSuppressesUnknownValue() {
        var original=snapshot(data(t -> row(t,"priceOffers","creator-offer").put("currency","KZT").put("amount","7500").put("upfrontAmount","7500")));
        var newer=snapshot(data(t -> row(t,"priceOffers","creator-offer").put("currency","KZT").put("amount","7550").put("upfrontAmount","7550")));
        assertFalse(notifies(original,newer,editor(),NOW));
        assertTrue(notifies(original,newer,editor(),NOW.plusSeconds(86401)));
    }

    @Test void smallKnownFeeChangeUsesTheSameFinancialThreshold() {
        var newer=snapshot(data(t -> row(t,"priceOffers","creator-offer").put("additionalFee","0.01")));
        assertFalse(notifies(snapshot(),newer,editor(),NOW));
    }

    private boolean notifies(com.aicompass.compass.catalog.PublishedCatalogSnapshot old,
            com.aicompass.compass.catalog.PublishedCatalogSnapshot next,RecommendationProfile profile,java.time.Instant at) {
        var delta=changes.compare(old,next,profile,Set.of("creator-plan"),at);
        var engine=new RecommendationEngine();
        return changes.significant(delta,old,next,engine.calculate(profile,old,at),engine.calculate(profile,next,at),at);
    }
    @Test void smallPriceChangeCannotHideBudgetViolation() {
        var profile=change(editor(),t -> ((tools.jackson.databind.node.ObjectNode)t.get("budget")).put("monthlyMax","15"));
        var newer=snapshot(data(t -> row(t,"priceOffers","creator-offer").put("amount","15.10").put("upfrontAmount","15.10")));
        assertEquals("MATCH",new RecommendationEngine().calculate(profile,snapshot(),NOW).status());
        assertNotEquals("MATCH",new RecommendationEngine().calculate(profile,newer,NOW).status());
        assertTrue(notifies(snapshot(),newer,profile,NOW));
    }
}

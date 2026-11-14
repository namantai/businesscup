package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.aicompass.compass.recommendation.EngineFixtures.*;

class RecommendationBenchmark {
    static PublishedCatalogSnapshot forty() { return candidates(40); }
    static PublishedCatalogSnapshot candidates(int count) {
        return snapshot(data(t -> {
            var product=row(t,"products","universal").deepCopy();var plan=row(t,"subscriptionPlans","universal-plan").deepCopy();
            var offer=row(t,"priceOffers","universal-offer").deepCopy();
            var assertions=new ArrayList<tools.jackson.databind.node.ObjectNode>();
            for(var a:t.get("capabilityAssertions"))if("universal-plan".equals(a.get("planId").asString()))assertions.add(((tools.jackson.databind.node.ObjectNode)a).deepCopy());
            var products=t.putArray("products");var plans=t.putArray("subscriptionPlans");var offers=t.putArray("priceOffers");var caps=t.putArray("capabilityAssertions");
            for(int i=0;i<count;i++) {
                String id=String.format(Locale.ROOT,"bench-%02d",i);
                products.add(product.deepCopy().put("id",id).put("slug",id));
                plans.add(plan.deepCopy().put("id",id+"-plan").put("productId",id).putNull("exclusivityGroupId"));
                offers.add(offer.deepCopy().put("id",id+"-offer").put("planId",id+"-plan").put("amount",String.valueOf(i+1)).put("upfrontAmount",String.valueOf(i+1)));
                for(var a:assertions)caps.add(a.deepCopy().put("id",id+"-"+a.get("capabilityId").asString()).put("planId",id+"-plan"));
            }
            for(String list:List.of("exclusivityGroups","agents","planAgentAccess","planModelAccess","quotaPools","usageLimits","eligibilityRules"))t.putArray(list);
        }));
    }
    @Test void benchmarkFortyCandidatesFourTools() throws Exception {
        var profile=change(student(),t -> ((tools.jackson.databind.node.ObjectNode)t.get("budget")).put("monthlyMax","1000").put("upfrontMax","1000"));
        var snapshot=forty();var engine=new RecommendationEngine();
        for(int i=0;i<5;i++)engine.calculate(profile,snapshot,NOW);
        long[] times=new long[20]; RecommendationResult last=null;
        for(int i=0;i<times.length;i++){long start=System.nanoTime();last=engine.calculate(profile,snapshot,NOW);times[i]=System.nanoTime()-start;}
        Arrays.sort(times); double p95=times[18]/1_000_000.0;
        String report="candidates=40 maxTools=4 warmup=5 measured=20 p50_ms="+times[9]/1_000_000.0+" p95_ms="+p95+" visited_search_nodes="+last.search().visitedCombinations()+" non_empty_combinations="+(last.search().visitedCombinations()-1)+" empty_root_nodes=1"+" java="+System.getProperty("java.version")+" cpus="+Runtime.getRuntime().availableProcessors()+" os="+System.getProperty("os.name")+" arch="+System.getProperty("os.arch");
        Files.writeString(Path.of("target/recommendation-benchmark.txt"),report+"\n");System.out.println(report);
        assertEquals(40,last.search().consideredCandidates());assertTrue(last.searchComplete());// Every candidate covers the fixture's requirements; no budgets/conflicts prune this fixture.
        long nonEmptyCombinations = 40L + 40L*39/2 + 40L*39*38/6 + 40L*39*38*37/24;
        assertEquals(102090, nonEmptyCombinations);
        assertEquals(nonEmptyCombinations + 1, last.search().visitedCombinations());
        assertEquals(nonEmptyCombinations, last.search().feasibleCombinations());
        assertTrue(p95<1000,report);
    }
}

package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import java.util.*;
import static com.aicompass.compass.catalog.CatalogTypes.*;

final class CapabilityCoverage {
    record Target(String id, int weight, boolean required) {}
    record State(CapabilityState state, boolean confirmed, boolean accepted, List<String> evidenceIds) {}
    private final PublishedCatalogSnapshot catalog;
    private final RecommendationProfile profile;
    private final EvidenceAssessment evidence;
    final List<Target> targets;
    final BitSet required = new BitSet();
    final int totalWeight;
    CapabilityCoverage(PublishedCatalogSnapshot catalog, RecommendationProfile profile, EvidenceAssessment evidence) {
        this.catalog = catalog; this.profile = profile; this.evidence = evidence;
        var map = new TreeMap<String,Target>();
        for (var need : profile.needs()) {
            var mappings = catalog.mappings().stream().filter(m -> m.needId().equals(need.id())).toList();
            if (mappings.isEmpty()) merge(map, new Target("need:" + need.id(), need.weight(), need.required()));
            for (var m : mappings) merge(map, new Target(m.capabilityId(), need.weight(), need.required()));
        }
        for (var f : profile.requirements().features()) merge(map, new Target(f.capabilityId(), f.weight(), f.required()));
        targets = List.copyOf(map.values());
        // Resource bound is explicit; the API cannot ask the solver to process an unbounded taxonomy.
        if (targets.size() > 256) throw ProfileValidator.bad("Requested taxonomy resolves to more than 256 functions; narrow the profile");
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).required()) required.set(i);
        totalWeight = targets.stream().mapToInt(Target::weight).sum();
    }
    private static void merge(Map<String,Target> map, Target t) {
        map.merge(t.id(), t, (a,b) -> new Target(a.id(), Math.max(a.weight(), b.weight()), a.required() || b.required()));
    }
    State state(String productId, String planId, String capability) {
        var assertions = catalog.assertions().stream().filter(a -> a.capabilityId().equals(capability)
                && Objects.equals(a.planId(), planId) && planId != null).toList();
        if (assertions.isEmpty()) assertions = catalog.assertions().stream().filter(a -> a.capabilityId().equals(capability)
                && Objects.equals(a.productId(), productId) && productId != null).toList();
        var ids = assertions.stream().map(CatalogManifest.CapabilityAssertion::evidenceId).sorted().distinct().toList();
        var verified = assertions.stream().filter(a -> evidence.confirmed(a.evidenceId())).toList();
        var values = verified.stream().map(CatalogManifest.CapabilityAssertion::state).distinct().toList();
        if (values.size() > 1) return new State(CapabilityState.UNKNOWN, false, false, ids);
        if (!verified.isEmpty()) {
            var state = values.getFirst();
            boolean accepted = state == CapabilityState.PARTIAL && verified.stream().anyMatch(a -> profile.acceptedConditionEvidenceIds().contains(a.evidenceId()));
            return new State(state, state == CapabilityState.SUPPORTED, accepted, ids);
        }
        boolean accepted = assertions.stream().anyMatch(a -> (a.state() == CapabilityState.SUPPORTED || a.state() == CapabilityState.PARTIAL)
                && evidence.accepted(a.evidenceId(), profile));
        boolean partial = assertions.stream().anyMatch(a -> a.state() == CapabilityState.PARTIAL && evidence.status(a.evidenceId()).equals("CONDITIONAL"));
        return new State(partial ? CapabilityState.PARTIAL : CapabilityState.UNKNOWN, false, accepted, ids);
    }
    String constraint(CapabilityCategory category, String value) {
        String prefix = switch (category) {
            case PLATFORM -> "platform-";
            case INTERFACE_LANGUAGE -> "interface-language-";
            case OUTPUT_LANGUAGE -> "output-language-";
            case PRIVACY -> "privacy-";
            default -> throw new IllegalArgumentException("Not a constraint category");
        };
        return prefix + value.toLowerCase(Locale.ROOT).replace('_', '-');
    }
    List<RecommendationResult.Coverage> combine(List<Candidate> candidates) {
        var result = new ArrayList<RecommendationResult.Coverage>();
        for (int i = 0; i < targets.size(); i++) {
            var target = targets.get(i); final int index = i;
            boolean supported = candidates.stream().anyMatch(c -> c.confirmed.get(index));
            boolean accepted = candidates.stream().anyMatch(c -> c.accepted.get(index));
            boolean partial = candidates.stream().anyMatch(c -> c.states.get(index).state() == CapabilityState.PARTIAL);
            boolean unknown = candidates.isEmpty() || candidates.stream().anyMatch(c -> c.states.get(index).state() == CapabilityState.UNKNOWN);
            CapabilityState state = supported ? CapabilityState.SUPPORTED : partial ? CapabilityState.PARTIAL : unknown ? CapabilityState.UNKNOWN : CapabilityState.UNSUPPORTED;
            result.add(new RecommendationResult.Coverage(target.id(), target.weight(), target.required(), state, supported, accepted,
                    candidates.stream().flatMap(c -> c.states.get(index).evidenceIds().stream()).sorted().distinct().toList()));
        }
        return List.copyOf(result);
    }
    int score(BitSet mask) {
        int weight = 0;
        for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1)) weight += targets.get(i).weight();
        return weight;
    }
}

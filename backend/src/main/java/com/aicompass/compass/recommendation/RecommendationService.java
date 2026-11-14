package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.CatalogReadService;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    private final ProfileValidator validator;
    private final CatalogReadService catalog;
    private final Clock clock;
    private final RecommendationEngine engine = new RecommendationEngine();
    public RecommendationService(ProfileValidator validator, CatalogReadService catalog, Clock clock) {
        this.validator = validator; this.catalog = catalog; this.clock = clock;
    }
    public RecommendationResult preview(String json) {
        var now = clock.instant();
        var profile = validator.read(json, now);
        var snapshot = catalog.currentSnapshot();
        return engine.calculate(profile, snapshot, profile.asOf() == null ? now : profile.asOf());
    }
}

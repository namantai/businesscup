package com.aicompass.compass.catalog;

import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.aicompass.compass.catalog.CatalogTypes.*;

class ReviewedManifestTest {
    @ParameterizedTest @ValueSource(strings = {"catalog-2026-11-14.json", "catalog-2026-11-14-r2.json"})
    void reviewedArtifactHasValidEvidenceAndDoesNotInventLocalPrices(String file) throws Exception {
        try (var input = getClass().getResourceAsStream("/data/catalog/" + file);
             var factory = Validation.buildDefaultValidatorFactory()) {
            assertNotNull(input);
            var manifest = new CatalogJson().read(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            assertFalse(manifest.asOf().isAfter(Clock.systemUTC().instant()));
            new CatalogImportValidator(factory.getValidator(), Clock.fixed(manifest.asOf(), ZoneOffset.UTC)).validate(manifest, false);
            assertEquals(DatasetKind.REVIEWED, manifest.datasetKind());
            assertTrue(manifest.evidenceSources().stream().allMatch(s -> s.kind() == SourceKind.OFFICIAL));
            assertTrue(manifest.priceOffers().stream().filter(o -> o.regionId().equals("kz"))
                    .allMatch(o -> o.priceKind() == PriceKind.UNKNOWN && o.amount() == null));
            if (file.endsWith("r2.json")) {
                assertTrue(manifest.capabilityAssertions().stream().filter(a -> a.state() == CapabilityState.UNSUPPORTED)
                        .allMatch(a -> a.capabilityId().equals("codex-image-generation")));
            }
            assertTrue(manifest.benchmarkObservations().isEmpty());
            assertTrue(manifest.fxSnapshots().isEmpty());
            assertTrue(manifest.priceOffers().stream().allMatch(o -> OfferCosts.from(o).allInUpfront().isEmpty()));
        }
    }
}

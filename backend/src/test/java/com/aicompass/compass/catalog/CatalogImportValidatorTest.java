package com.aicompass.compass.catalog;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import static com.aicompass.compass.catalog.CatalogTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogImportValidatorTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final CatalogJson json = new CatalogJson();
    private final CatalogImportValidator validator = new CatalogImportValidator(FACTORY.getValidator(), CLOCK);
    @AfterAll static void closeValidator() { FACTORY.close(); }

    @Test void acceptsLabeledSyntheticFixture() { validator.validate(json.read(fixture()), true); }
    @Test void rejectsDemoWithoutExplicitPermission() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> validator.validate(json.read(fixture()), false)).getMessage().contains("DEMO disabled"));
    }
    @Test void preservesDecimalStringsOnRoundTrip() {
        assertEquals(json.read(fixture()), json.read(json.write(json.read(fixture()))));
        assertTrue(json.write(json.read(fixture())).contains("\"amount\":\"0.000123\""));
    }
    @Test void forbidsDemoFlagInProductionAndMixedProfiles() {
        for (String[] profiles : new String[][]{{}, {"prod"}, {"test", "prod"}, {"staging"}}) {
            var env = new MockEnvironment().withProperty("compass.catalog.allow-demo", "true");
            env.setActiveProfiles(profiles);
            assertThrows(IllegalStateException.class, () -> new CatalogPolicy(env, null));
        }
        var env = new MockEnvironment().withProperty("compass.catalog.allow-demo", "true");
        env.setActiveProfiles("test");
        assertTrue(new CatalogPolicy(env, null).allowDemo());
        assertFalse(new CatalogPolicy(new MockEnvironment(), null).allowDemo());
    }
    @Test void rejectsDuplicateJsonKeys() {
        assertThrows(IllegalArgumentException.class, () -> json.read(fixture().replace("\"schemaVersion\": 1", "\"schemaVersion\": 1, \"schemaVersion\": 1")));
    }
    @Test void rejectsTrailingJson() { assertThrows(IllegalArgumentException.class, () -> json.read(fixture() + "{}")); }

    static Stream<Arguments> invalidManifests() {
        return Stream.of(
            bad("timestamp is not an epoch number", d -> d.put("asOf",1768478400)),
            bad("unknown price is not zero", d -> row(d,"priceOffers",3).put("amount","0")),
            bad("unknown evidence is not a free price", d -> row(d,"priceOffers",2).put("priceEvidenceId","demo-e-unknown-price")),
            bad("annual upfront cannot be monthly equivalent", d -> row(d,"priceOffers",1).put("upfrontAmount","10")),
            bad("currency is mandatory", d -> row(d,"priceOffers",0).remove("currency")),
            bad("currency must be monetary ISO", d -> row(d,"priceOffers",0).put("currency","ZZZ")),
            bad("money is not a JSON number", d -> row(d,"priceOffers",0).put("amount",12.34)),
            bad("too much precision", d -> row(d,"priceOffers",0).put("amount","0.0000001")),
            bad("no negative money", d -> row(d,"priceOffers",0).put("amount","-1")),
            bad("null mandatory field", d -> row(d,"priceOffers",0).putNull("planId")),
            bad("broken plan reference", d -> row(d,"priceOffers",0).put("planId","missing")),
            bad("duplicate stable id", d -> ((ArrayNode)d.get("products")).add(row(d,"products",0).deepCopy())),
            bad("unknown JSON property", d -> d.put("inventedScore",96)),
            bad("missing list", d -> d.remove("priceOffers")),
            bad("future evidence", d -> row(d,"evidence",0).put("verifiedAt","2030-01-01T00:00:00Z")),
            bad("future catalog", d -> d.put("asOf","2030-01-01T00:00:00Z")),
            bad("reviewed cannot contain demo", d -> d.put("datasetKind","REVIEWED")),
            bad("demo cannot declare verified market fact", d -> row(d,"evidence",0).put("status","VERIFIED_SOURCE")),
            bad("source date required", d -> row(d,"evidence",0).putNull("verifiedAt")),
            bad("source cannot contain credentials", d -> row(d,"evidenceSources",0).put("url","https://user:pass@fixtures.example.invalid/x")),
            bad("evidence for wrong field", d -> row(d,"priceOffers",0).put("priceEvidenceId","demo-e-monthly-tax")),
            bad("unknown fee is not zero", d -> row(d,"priceOffers",9).put("additionalFee","0")),
            bad("trial needs expiration duration", d -> row(d,"priceOffers",5).putNull("trialDays")),
            bad("pool belongs to another offer", d -> row(d,"usageLimits",0).put("offerId","demo-offer-free")),
            bad("tier conflict cannot be vendor wide", d -> row(d,"subscriptionPlans",0).put("exclusivityGroupId","demo-workbench-tiers")),
            bad("assertion cannot target both product and plan", d -> row(d,"capabilityAssertions",0).put("productId","demo-studio")),
            bad("one-time credits require expiry", d -> row(d,"quotaPools",1).putNull("expiresAt")),
            bad("unknown limit cannot be zero", d -> row(d,"quotaPools",2).put("quantity","0")),
            bad("unknown capability cannot imply absence", d -> row(d,"capabilityAssertions",2).put("state","UNSUPPORTED")),
            bad("unknown capability cannot imply support", d -> row(d,"capabilityAssertions",2).put("state","SUPPORTED")),
            bad("benchmark needs exact model reference", d -> row(d,"benchmarkObservations",0).put("modelId","demo-family")),
            bad("invalid FX rate", d -> row(d,"fxSnapshots",0).put("rate","0")),
            bad("null array row", d -> ((ArrayNode)d.get("products")).addNull())
        );
    }
    private static Arguments bad(String name, Consumer<ObjectNode> mutation) { return Arguments.of(name, mutation); }
    @ParameterizedTest(name="{0}") @MethodSource("invalidManifests")
    void rejectsInvalidManifest(String name, Consumer<ObjectNode> mutate) {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(json.read(mutated(mutate)), true), name);
    }
}

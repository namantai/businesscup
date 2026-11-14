package com.aicompass.compass.shared;

import com.aicompass.compass.catalog.OfferCosts;
import com.aicompass.compass.shared.money.FxConversion;
import com.aicompass.compass.shared.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import static com.aicompass.compass.catalog.CatalogTestData.*;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test void addsExactly() { assertEquals(Money.of("0.3", "USD"), Money.of("0.1", "USD").add(Money.of("0.2", "USD"))); }
    @Test void moneyJsonUsesDecimalStringsWithoutScientificNotation() {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        assertEquals("1000", mapper.readTree(mapper.writeValueAsString(Money.of("1000", "USD"))).get("amount").asString());
        assertTrue(mapper.readTree(mapper.writeValueAsString(Money.of("0.000123", "USD"))).get("amount").isString());
        assertEquals("USD", mapper.readTree(mapper.writeValueAsString(Money.of("1000", "USD"))).get("currency").asString());
    }
    @Test void preservesMicroPrices() { assertEquals("0.000123", Money.of("0.000123", "USD").decimalString()); }
    @Test void rejectsUnknownAndNegative() {
        assertThrows(NullPointerException.class, () -> Money.of(null, "USD"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("-0.01", "USD"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("0.0000001", "USD"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("1", "XXX"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("1", "ZZZ"));
    }
    @Test void refusesImplicitFx() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("1", "USD").add(Money.of("1", "KZT")));
        assertThrows(IllegalArgumentException.class, () -> Money.of("1", "USD").atMost(Money.of("2", "KZT")));
    }
    @Test void annualEquivalentDoesNotRemoveUpfront() {
        var costs = OfferCosts.from(offer("annual"));
        assertEquals(Money.of("10.000834", "USD"), costs.recurringMonthly().orElseThrow());
        assertEquals(Money.of("120.01", "USD"), costs.upfront().orElseThrow());
        assertFalse(costs.upfront().orElseThrow().atMost(Money.of("30", "USD")));
    }
    @Test void freeIsZeroButUnknownHasNoComponents() {
        assertEquals(Money.of("0", "USD"), OfferCosts.from(offer("free")).recurringMonthly().orElseThrow());
        var unknown = OfferCosts.from(offer("unknown"));
        assertTrue(unknown.recurringMonthly().isEmpty());
        assertTrue(unknown.allInUpfront().isEmpty());
    }
    @Test void trialAndMeteredPriceAreNotRecurringFreeAllowance() {
        var trial = OfferCosts.from(offer("trial"));
        assertTrue(trial.temporary()); assertTrue(trial.recurringMonthly().isEmpty());
        var metered = OfferCosts.from(offer("metered"));
        assertTrue(metered.usageBased()); assertTrue(metered.recurringMonthly().isEmpty());
    }
    @Test void addsKnownTaxAndFeesOncePerBillingPeriod() {
        var changed = new com.aicompass.compass.catalog.CatalogJson().read(mutated(d -> row(d,"priceOffers",0)
                .put("taxStatus","EXCLUDED_KNOWN").put("additionalTax","1.99").put("additionalFee","0.50")));
        var costs = OfferCosts.from(changed.priceOffers().getFirst());
        assertEquals(Money.of("14.83", "USD"), costs.allInRecurringMonthly().orElseThrow());
        assertEquals(Money.of("14.83", "USD"), costs.allInUpfront().orElseThrow());
    }
    @Test void unknownTaxOrFeeCannotProduceAllInCost() {
        for (String scenario : new String[]{"tax-unknown", "fee-unknown"}) {
            var costs = OfferCosts.from(offer(scenario));
            assertTrue(costs.recurringMonthly().isPresent());
            assertTrue(costs.allInRecurringMonthly().isEmpty());
            assertTrue(costs.allInUpfront().isEmpty());
        }
    }
    @Test void fxRoundingIsExplicit() {
        var snapshot = fx("1.005", "USD", "EUR", CLOCK.instant());
        assertEquals(Money.of("1.00", "EUR"), FxConversion.convert(Money.of("1", "USD"), snapshot, CLOCK.instant(), RoundingMode.HALF_EVEN));
        assertEquals(Money.of("1.01", "EUR"), FxConversion.convert(Money.of("1", "USD"), snapshot, CLOCK.instant(), RoundingMode.CEILING));
    }
    @Test void fxHonorsTargetCurrencyDigits() {
        assertEquals(Money.of("2", "JPY"), FxConversion.convert(Money.of("1", "USD"), fx("1.5", "USD", "JPY", CLOCK.instant()), CLOCK.instant(), RoundingMode.CEILING));
    }
    @Test void fxRejectsFutureStaleWrongPairAndInvalidRate() {
        for (Instant time : new Instant[]{CLOCK.instant().plusSeconds(1), CLOCK.instant().minusSeconds(86400)}) {
            assertThrows(IllegalArgumentException.class, () -> FxConversion.convert(Money.of("1", "USD"), fx("2", "USD", "KZT", time), CLOCK.instant(), RoundingMode.CEILING));
        }
        assertThrows(IllegalArgumentException.class, () -> FxConversion.convert(Money.of("1", "EUR"), fx("2", "USD", "KZT", CLOCK.instant()), CLOCK.instant(), RoundingMode.CEILING));
        assertThrows(IllegalArgumentException.class, () -> fx("0", "USD", "KZT", CLOCK.instant()));
    }
    private static FxConversion.Snapshot fx(String rate, String base, String quote, Instant time) {
        return new FxConversion.Snapshot("synthetic-rate", Currency.getInstance(base), Currency.getInstance(quote), new BigDecimal(rate), time);
    }
}

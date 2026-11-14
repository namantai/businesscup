package com.aicompass.compass.catalog;

import com.aicompass.compass.shared.money.Money;
import java.util.Optional;
import static com.aicompass.compass.catalog.CatalogTypes.*;

/** Components only; eligibility, retained subscriptions and bundle selection belong to later stages. */
public record OfferCosts(Optional<Money> recurringMonthly, Optional<Money> upfront,
                         Optional<Money> allInRecurringMonthly, Optional<Money> allInUpfront,
                         boolean temporary, boolean usageBased) {
    public static OfferCosts from(CatalogManifest.PriceOffer offer) {
        if (offer.priceKind() == PriceKind.UNKNOWN || offer.priceKind() == PriceKind.USAGE_BASED) {
            return new OfferCosts(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    false, offer.priceKind() == PriceKind.USAGE_BASED);
        }
        Money charge = Money.of(offer.amount(), offer.currency());
        Money upfront = Money.of(offer.upfrontAmount(), offer.currency());
        int months = offer.billingPeriod() == BillingPeriod.YEAR ? 12 : 1;
        boolean recurring = offer.billingPeriod() == BillingPeriod.MONTH || offer.billingPeriod() == BillingPeriod.YEAR;
        boolean extrasKnown = offer.taxStatus() != TaxStatus.UNKNOWN && offer.feeStatus() == ValueStatus.KNOWN;
        Optional<Money> extras = extrasKnown
                ? Optional.of(Money.of(offer.additionalTax(), offer.currency()).add(Money.of(offer.additionalFee(), offer.currency())))
                : Optional.empty();
        return new OfferCosts(recurring ? Optional.of(charge.monthlyEquivalent(months)) : Optional.empty(),
                Optional.of(upfront),
                recurring ? extras.map(value -> charge.add(value).monthlyEquivalent(months)) : Optional.empty(),
                extras.map(upfront::add), offer.priceKind() == PriceKind.TRIAL, false);
    }
}

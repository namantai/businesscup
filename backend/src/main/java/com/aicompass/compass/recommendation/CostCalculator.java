package com.aicompass.compass.recommendation;

import com.aicompass.compass.catalog.*;
import com.aicompass.compass.shared.money.*;
import com.aicompass.compass.shared.time.RenewalSchedule;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.aicompass.compass.recommendation.RecommendationResult.*;
import static com.aicompass.compass.catalog.CatalogTypes.*;

final class CostCalculator {
    record Amounts(BigDecimal recurring, BigDecimal now, BigDecimal upfront, BigDecimal horizon,
                   boolean confirmed, List<Reason> reasons, List<FxUsed> fx) {}
    private final PublishedCatalogSnapshot catalog;
    private final EvidenceAssessment evidence;
    private final RecommendationProfile profile;
    private final Instant asOf;
    CostCalculator(PublishedCatalogSnapshot catalog, EvidenceAssessment evidence, RecommendationProfile profile, Instant asOf) {
        this.catalog = catalog; this.evidence = evidence; this.profile = profile; this.asOf = asOf;
    }
    Amounts offer(CatalogManifest.PriceOffer offer) {
        var reasons = new ArrayList<Reason>(); var used = new ArrayList<FxUsed>();
        boolean known = offer.amount() != null && offer.upfrontAmount() != null
                && offer.taxStatus() != TaxStatus.UNKNOWN && offer.feeStatus() != ValueStatus.UNKNOWN;
        boolean verified = List.of(offer.priceEvidenceId(), offer.taxEvidenceId(), offer.feeEvidenceId()).stream().allMatch(evidence::confirmed);
        if (!known || !verified) reasons.add(reason(Code.PRICE_INCOMPLETE, "Price, taxes or fees are unverified; no confirmed all-in budget fit", offer.priceEvidenceId(), offer.taxEvidenceId(), offer.feeEvidenceId()));
        BigDecimal charge = known ? new BigDecimal(offer.amount()).add(new BigDecimal(offer.additionalTax())).add(new BigDecimal(offer.additionalFee())) : null;
        BigDecimal first = known ? new BigDecimal(offer.upfrontAmount()).add(new BigDecimal(offer.additionalTax())).add(new BigDecimal(offer.additionalFee())) : null;
        charge = convert(charge, offer.currency(), used, reasons); first = convert(first, offer.currency(), used, reasons);
        boolean recurring = offer.billingPeriod() == BillingPeriod.MONTH || offer.billingPeriod() == BillingPeriod.YEAR;
        int period = offer.billingPeriod() == BillingPeriod.YEAR ? 12 : 1;
        BigDecimal monthly = recurring && charge != null ? charge.divide(BigDecimal.valueOf(period), 6, RoundingMode.CEILING) : null;
        BigDecimal total = first;
        if (recurring && charge != null && first != null) {
            for (int month = period; month < profile.horizonMonths(); month += period) total = total.add(charge);
        } else if (!recurring) {
            monthly = null; total = null;
            reasons.add(reason(offer.priceKind() == PriceKind.TRIAL ? Code.TRIAL_NOT_RECURRING : Code.PRICE_INCOMPLETE,
                    "One-time/trial/usage charges cannot establish sustainable recurring cost", offer.priceEvidenceId()));
        }
        var horizonEnd = asOf.atZone(ZoneOffset.UTC).plusMonths(profile.horizonMonths()).toInstant();
        var plan = catalog.plans().stream().filter(p -> p.id().equals(offer.planId())).findFirst().orElseThrow();
        if ((offer.validUntil() != null && offer.validUntil().isBefore(horizonEnd))
                || (plan.activeUntil() != null && plan.activeUntil().isBefore(horizonEnd))) {
            total = null; verified = false;
            reasons.add(reason(Code.PRICE_INCOMPLETE, "Known offer/plan expiry precedes the requested horizon; renewal price is unknown", offer.priceEvidenceId()));
        }
        return new Amounts(monthly, first, first, total, known && verified && monthly != null, List.copyOf(reasons), List.copyOf(used));
    }
    Amounts existing(RecommendationProfile.ExistingSubscription s) {
        var reasons = new ArrayList<Reason>(); var used = new ArrayList<FxUsed>();
        reasons.add(reason(Code.USER_SUPPLIED_COST, "Existing amount is user supplied, including taxes/fees as reported; not provider-verified"));
        BigDecimal charge = convert(s.amount() == null ? null : new BigDecimal(s.amount()), s.currency(), used, reasons);
        if (charge == null) reasons.add(reason(Code.PRICE_INCOMPLETE, "Existing subscription cost or FX is unknown"));
        int period = s.billingPeriod() == RecommendationProfile.Period.YEAR ? 12 : 1;
        var schedule = new RenewalSchedule(s.renewalAnchor(), ZoneId.of(s.billingZone()), period);
        LocalDate today = asOf.atZone(ZoneId.of(s.billingZone())).toLocalDate();
        // Renewal inputs and cash-due-now have day precision. Exclude the final
        // calendar date in the same billing zone, regardless of calculation hour.
        Instant end = today.plusMonths(profile.horizonMonths()).atStartOfDay(ZoneId.of(s.billingZone())).toInstant();
        BigDecimal now = s.nextRenewal().equals(today) ? charge : BigDecimal.ZERO;
        BigDecimal total = charge == null ? null : BigDecimal.ZERO;
        long anchorMonths = java.time.temporal.ChronoUnit.MONTHS.between(s.renewalAnchor().withDayOfMonth(1), s.nextRenewal().withDayOfMonth(1));
        long startIndex = anchorMonths / period;
        for (long i = startIndex; i <= startIndex + 24; i++) {
            Instant renewal = schedule.occurrence(i);
            if (!renewal.isBefore(end)) break;
            if (!renewal.isBefore(today.atStartOfDay(ZoneId.of(s.billingZone())).toInstant()) && total != null) total = total.add(charge);
        }
        return new Amounts(charge == null ? null : charge.divide(BigDecimal.valueOf(period), 6, RoundingMode.CEILING),
                now, now, total, charge != null, List.copyOf(reasons), List.copyOf(used));
    }
    BigDecimal convert(BigDecimal amount, String currency, List<FxUsed> used, List<Reason> reasons) {
        if (amount == null) return null;
        if (currency.equals(profile.budget().currency()) || amount.signum() == 0) return amount;
        var fx = catalog.fx().stream().filter(f -> f.baseCurrency().equals(currency) && f.quoteCurrency().equals(profile.budget().currency())
                && evidence.confirmed(f.evidenceId()) && !f.observedAt().isAfter(asOf)
                && Duration.between(f.observedAt(), asOf).compareTo(Duration.ofHours(24)) < 0)
                .sorted(Comparator.comparing(CatalogManifest.FxSnapshot::observedAt).reversed().thenComparing(CatalogManifest.FxSnapshot::id)).findFirst();
        if (fx.isEmpty()) { reasons.add(reason(Code.FX_MISSING, "No confirmed current direct FX snapshot for " + currency)); return null; }
        var f = fx.get();
        try {
            var converted = FxConversion.convert(new Money(amount, Currency.getInstance(currency)),
                    new FxConversion.Snapshot(f.id(), Currency.getInstance(f.baseCurrency()), Currency.getInstance(f.quoteCurrency()),
                            new BigDecimal(f.rate()), f.observedAt()), asOf, RoundingMode.CEILING);
            var record = new FxUsed(f.id(), f.baseCurrency(), f.quoteCurrency(), f.rate(), f.observedAt(), f.evidenceId());
            if (!used.contains(record)) used.add(record);
            return converted.amount();
        } catch (IllegalArgumentException error) {
            reasons.add(reason(Code.FX_MISSING, "FX conversion exceeds supported monetary bounds", f.evidenceId())); return null;
        }
    }
    static BigDecimal add(BigDecimal a, BigDecimal b) { return a == null || b == null ? null : a.add(b); }
    static String decimal(BigDecimal value) { return value == null ? null : value.stripTrailingZeros().toPlainString(); }
    static Reason reason(Code code, String detail, String... ids) { return new Reason(code, detail, List.of(ids)); }
}

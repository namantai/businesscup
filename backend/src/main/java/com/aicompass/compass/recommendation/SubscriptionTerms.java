package com.aicompass.compass.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;
import com.aicompass.compass.shared.money.Money;

/** User ledger terms, never evidence about provider capabilities. Null means unknown. */
public record SubscriptionTerms(
        @NotBlank @Size(max=100) String name,
        @NotNull Status status,
        @Schema(nullable=true) UUID catalogVersion,
        @Schema(nullable=true) @Size(max=80) String planId,
        @Schema(nullable=true) @Size(max=80) String offerId,
        @NotBlank String currency,
        @Schema(nullable=true) String amount,
        @NotNull PriceOrigin priceOrigin,
        @NotNull RecommendationProfile.Period billingPeriod,
        @Schema(nullable=true) LocalDate renewalAnchor,
        @Schema(nullable=true) LocalDate nextRenewal,
        @NotBlank String billingZone,
        @Schema(nullable=true) LocalDate paidFrom,
        @Schema(nullable=true) LocalDate paidUntil,
        @Schema(nullable=true) String paidAmount,
        @NotNull Renewal autoRenew,
        @Schema(nullable=true) LocalDate trialUntil,
        @NotNull Obligation obligation,
        @Schema(nullable=true) LocalDate commitmentUntil,
        @Schema(nullable=true) LocalDate cancellationAvailableFrom,
        @Schema(nullable=true) String additionalPerPeriod,
        @Schema(nullable=true) String transitionCost,
        boolean mustKeep) {
    public enum Status { ACTIVE, TRIAL, ENDED, UNKNOWN }
    public enum PriceOrigin { USER_REPORTED, CATALOG, UNKNOWN }
    public enum Renewal { YES, NO, UNKNOWN }
    public enum Obligation { NONE, UNTIL, UNKNOWN }
    public void validate() {
        try {
            Money.of("0",currency); ZoneId.of(billingZone);
            // Keep the API calendar within the four-digit ISO dates stored by V7.
            for (LocalDate date : new LocalDate[]{renewalAnchor, nextRenewal, paidFrom, paidUntil,
                    trialUntil, commitmentUntil, cancellationAvailableFrom}) {
                if (date != null && (date.getYear() < 1 || date.getYear() > 9999))
                    throw new IllegalArgumentException();
            }
            for(String value : new String[]{amount,paidAmount,additionalPerPeriod,transitionCost}) if(value!=null) {
                if(!value.matches("(0|[1-9][0-9]{0,8})(\\.[0-9]{1,6})?")) throw new IllegalArgumentException();
                Money.of(value,currency);
            }
            if((priceOrigin==PriceOrigin.UNKNOWN)!=(amount==null)) throw new IllegalArgumentException();
            if((catalogVersion==null)!=(planId==null)||(planId==null)!=(offerId==null))throw new IllegalArgumentException();
            if(priceOrigin==PriceOrigin.CATALOG && offerId==null)throw new IllegalArgumentException();
            if((renewalAnchor==null)!=(nextRenewal==null))throw new IllegalArgumentException();
            if(renewalAnchor!=null) {
                var schedule=new com.aicompass.compass.shared.time.RenewalSchedule(renewalAnchor,ZoneId.of(billingZone),billingPeriod==RecommendationProfile.Period.YEAR?12:1);
                if(renewalAnchor.isAfter(nextRenewal)||!schedule.nextOnOrAfter(nextRenewal.atStartOfDay(ZoneId.of(billingZone)).toInstant()).atZone(ZoneId.of(billingZone)).toLocalDate().equals(nextRenewal))throw new IllegalArgumentException();
            }
            if(paidFrom!=null && (paidUntil==null||!paidFrom.isBefore(paidUntil)))throw new IllegalArgumentException();
            if((obligation==Obligation.UNTIL)!=(commitmentUntil!=null))throw new IllegalArgumentException();
            if(paidUntil!=null && nextRenewal!=null && nextRenewal.isBefore(paidUntil))throw new IllegalArgumentException();
            if(trialUntil!=null && nextRenewal!=null && nextRenewal.isBefore(trialUntil))throw new IllegalArgumentException();
        } catch(IllegalArgumentException | DateTimeException ex) {throw ProfileValidator.bad("Invalid subscription money, provenance or calendar terms");}
    }
}

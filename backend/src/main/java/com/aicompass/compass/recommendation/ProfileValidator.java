package com.aicompass.compass.recommendation;

import com.aicompass.compass.shared.money.Money;
import jakarta.validation.Validator;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.type.LogicalType;

@Component
public class ProfileValidator {
    private final Validator validator;
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .withCoercionConfig(LogicalType.Textual, c -> c.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail).setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();
    public ProfileValidator(Validator validator) { this.validator = validator; }
    public RecommendationProfile read(String json, Instant now) {
        RecommendationProfile profile;
        try {
            if (json.length() > 65536) throw bad("Profile exceeds 64 KiB");
            var tree = mapper.readTree(json);
            var timestamp = tree.get("asOf");
            if (timestamp != null && !timestamp.isNull() && (!timestamp.isString() || !timestamp.asString().endsWith("Z")))
                throw bad("asOf must be a UTC ISO timestamp string");
            var subscriptions = tree.get("existingSubscriptions");
            if (subscriptions != null && subscriptions.isArray()) for (var entry : subscriptions) {
                for (String field : List.of("renewalAnchor", "nextRenewal")) {
                    var date = entry.get(field);
                    if (date != null && !date.isString()) throw bad("Calendar dates must be ISO strings");
                }
            }
            profile = mapper.readValue(json, RecommendationProfile.class);
        } catch (RuntimeException error) { throw bad("Invalid profile JSON: required fields, enums, decimal strings and unique keys expected"); }
        validate(profile, now);
        return profile;
    }
    public void validate(RecommendationProfile p, Instant now) {
        if (p == null) throw bad("Profile is required");
        var errors = validator.validate(p);
        if (!errors.isEmpty()) throw bad("Invalid fields: " + errors.stream().map(e -> e.getPropertyPath().toString()).sorted().toList());
        try {
            if (!Set.of(Locale.getISOCountries()).contains(p.country())) throw bad("Unknown ISO country");
            decimal(p.budget().monthlyMax(), p.budget().currency()); decimal(p.budget().upfrontMax(), p.budget().currency());
            unique(p.needs(), RecommendationProfile.Need::id); unique(p.requirements().features(), RecommendationProfile.Feature::capabilityId);
            unique(p.requirements().usage(), u -> u.capabilityId() + ":" + u.mode());
            unique(p.existingSubscriptions(), RecommendationProfile.ExistingSubscription::id);
            unique(p.existingSubscriptions().stream().filter(s -> s.planId() != null).toList(), RecommendationProfile.ExistingSubscription::planId);
            unique(p.outputLanguages(), Function.identity()); unique(p.requirements().platforms(), Function.identity());
            unique(p.acceptedConditionEvidenceIds(), Function.identity());
            p.requirements().usage().forEach(u -> decimal(u.monthlyQuantity(), p.budget().currency()));
            for (var s : p.existingSubscriptions()) {
                Money.of("0", s.currency());
                if (s.amount() != null) decimal(s.amount(), s.currency());
                ZoneId.of(s.billingZone());
                if ((s.planId() == null) != (s.offerId() == null)) throw bad("Known subscriptions need both planId and offerId; custom entries use null for both");
                LocalDate date = (p.asOf() == null ? now : p.asOf()).atZone(ZoneId.of(s.billingZone())).toLocalDate();
                if (s.renewalAnchor().isAfter(s.nextRenewal())) throw bad("renewalAnchor cannot follow nextRenewal");
                var schedule = new com.aicompass.compass.shared.time.RenewalSchedule(s.renewalAnchor(), ZoneId.of(s.billingZone()), s.billingPeriod() == RecommendationProfile.Period.YEAR ? 12 : 1);
                if (!schedule.nextOnOrAfter(date.atStartOfDay(ZoneId.of(s.billingZone())).toInstant()).atZone(ZoneId.of(s.billingZone())).toLocalDate().equals(s.nextRenewal()))
                    throw bad("nextRenewal does not match the original renewalAnchor and billing period");
                if (s.nextRenewal().isBefore(date) || s.nextRenewal().isAfter(date.plusYears(2))) throw bad("nextRenewal must be within the next two years");
            }
            if (p.asOf() != null && (p.asOf().isAfter(now) || p.asOf().isBefore(now.minus(Duration.ofDays(365)))))
                throw bad("asOf must be within the past year, not in the future");
        } catch (IllegalArgumentException error) { throw bad("Invalid monetary value, currency, duplicate or calendar setting"); }
    }
    private static void decimal(String value, String currency) {
        if (!value.matches("(0|[1-9][0-9]{0,8})(\\.[0-9]{1,6})?")) throw bad("Decimal string out of range");
        Money.of(value, currency);
    }
    private static <T> void unique(List<T> list, Function<T,String> id) {
        if (list.stream().map(id).distinct().count() != list.size()) throw bad("Duplicate profile entry");
    }
    static ResponseStatusException bad(String detail) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail); }
}

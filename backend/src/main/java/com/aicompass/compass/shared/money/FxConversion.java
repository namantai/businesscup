package com.aicompass.compass.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

public final class FxConversion {
    private FxConversion() {}

    public record Snapshot(String id, Currency base, Currency quote, BigDecimal rate, Instant observedAt) {
        public Snapshot {
            Objects.requireNonNull(id);
            Objects.requireNonNull(base);
            Objects.requireNonNull(quote);
            Objects.requireNonNull(rate);
            Objects.requireNonNull(observedAt);
            if (id.isBlank() || base.equals(quote) || rate.signum() <= 0 || rate.scale() > 12
                    || rate.compareTo(new BigDecimal("1000000000000")) >= 0) {
                throw new IllegalArgumentException("Invalid explicit FX snapshot");
            }
        }
    }

    // The caller supplies a reviewed snapshot (or a labeled synthetic test snapshot).
    // This helper neither fetches rates nor infers missing or inverse rates.
    public static Money convert(Money source, Snapshot snapshot, Instant asOf, RoundingMode rounding) {
        Objects.requireNonNull(asOf);
        Objects.requireNonNull(rounding);
        if (!source.currency().equals(snapshot.base()) || snapshot.observedAt().isAfter(asOf)
                || Duration.between(snapshot.observedAt(), asOf).compareTo(Duration.ofHours(24)) >= 0) {
            throw new IllegalArgumentException("FX currency mismatch, future or stale snapshot");
        }
        int scale = snapshot.quote().getDefaultFractionDigits();
        if (scale < 0) throw new IllegalArgumentException("Unsupported monetary currency");
        return new Money(source.amount().multiply(snapshot.rate()).setScale(scale, rounding), snapshot.quote());
    }
}

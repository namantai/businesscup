package com.aicompass.compass.shared.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record RenewalSchedule(LocalDate anchorDate, ZoneId billingZone, int periodMonths) {
    public RenewalSchedule {
        Objects.requireNonNull(anchorDate);
        Objects.requireNonNull(billingZone);
        if (periodMonths != 1 && periodMonths != 12) throw new IllegalArgumentException("Monthly or annual renewal required");
    }

    public Instant occurrence(long index) {
        if (index < 0) throw new IllegalArgumentException("Negative renewal index");
        // Always count from the original calendar day: Jan 31 -> Feb 28 -> Mar 31.
        return anchorDate.plusMonths(Math.multiplyExact(index, periodMonths)).atStartOfDay(billingZone).toInstant();
    }

    public Instant nextOnOrAfter(Instant asOf) {
        long months = ChronoUnit.MONTHS.between(anchorDate.withDayOfMonth(1), asOf.atZone(billingZone).toLocalDate().withDayOfMonth(1));
        long index = Math.max(0, months / periodMonths);
        while (occurrence(index).isBefore(asOf)) index++;
        return occurrence(index);
    }
}

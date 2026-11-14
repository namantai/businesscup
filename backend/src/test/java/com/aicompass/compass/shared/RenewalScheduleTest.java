package com.aicompass.compass.shared;

import com.aicompass.compass.shared.time.RenewalSchedule;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenewalScheduleTest {
    @Test void monthEndDoesNotDrift() {
        var schedule = new RenewalSchedule(LocalDate.of(2025, 1, 31), ZoneOffset.UTC, 1);
        assertEquals(Instant.parse("2025-02-28T00:00:00Z"), schedule.occurrence(1));
        assertEquals(Instant.parse("2025-03-31T00:00:00Z"), schedule.occurrence(2));
    }
    @Test void leapYearUsesOriginalCalendarAnchor() {
        var schedule = new RenewalSchedule(LocalDate.of(2024, 2, 29), ZoneOffset.UTC, 12);
        assertEquals(Instant.parse("2025-02-28T00:00:00Z"), schedule.occurrence(1));
        assertEquals(Instant.parse("2028-02-29T00:00:00Z"), schedule.occurrence(4));
    }
    @Test void zoneIsPreservedWhenConvertingToUtc() {
        var schedule = new RenewalSchedule(LocalDate.of(2026, 1, 31), ZoneId.of("Asia/Almaty"), 1);
        assertEquals(Instant.parse("2026-02-27T19:00:00Z"), schedule.occurrence(1));
    }
    @Test void boundaryAndNextRenewalAreExplicit() {
        var schedule = new RenewalSchedule(LocalDate.of(2025, 1, 31), ZoneOffset.UTC, 1);
        assertEquals(schedule.occurrence(1), schedule.nextOnOrAfter(schedule.occurrence(1)));
        assertEquals(schedule.occurrence(2), schedule.nextOnOrAfter(schedule.occurrence(1).plusSeconds(1)));
        assertEquals(schedule.occurrence(0), schedule.nextOnOrAfter(Instant.parse("2020-01-01T00:00:00Z")));
    }
    @Test void invalidPeriodAndNegativeIndexAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RenewalSchedule(LocalDate.of(2026, 1, 1), ZoneOffset.UTC, 0));
        assertThrows(IllegalArgumentException.class, () -> new RenewalSchedule(LocalDate.of(2026, 1, 1), ZoneOffset.UTC, 1).occurrence(-1));
    }
}

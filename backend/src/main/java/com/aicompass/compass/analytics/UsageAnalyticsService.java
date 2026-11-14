package com.aicompass.compass.analytics;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@Transactional
public class UsageAnalyticsService {
    public enum Event { RESULT_SAVED, CHANGE_NOTICE_VIEWED, REVISIT }
    public record DailyEvent(Event event, LocalDate day) {}
    public record AnalyticsPreferences(boolean enabled,int retentionDays,List<DailyEvent> events) {}
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public UsageAnalyticsService(JdbcTemplate jdbc,Clock clock) { this.jdbc=jdbc;this.clock=clock; }
    private LocalDate today() { return clock.instant().atZone(ZoneOffset.UTC).toLocalDate(); }
    private boolean lockOwner(UUID owner) {
        return !jdbc.queryForList("SELECT id FROM compass.user_account WHERE id=? AND verified_at IS NOT NULL FOR UPDATE",UUID.class,owner).isEmpty();
    }
    public AnalyticsPreferences preferences(UUID owner) {
        boolean enabled=jdbc.queryForList("SELECT enabled FROM compass.analytics_consent WHERE owner_id=?",Boolean.class,owner).stream().findFirst().orElse(false);
        var events=jdbc.query("SELECT event_name,event_day FROM compass.product_event WHERE owner_id=? AND event_day>=? ORDER BY event_day,event_name",
                (r,n)->new DailyEvent(Event.valueOf(r.getString("event_name")),r.getObject("event_day",LocalDate.class)),owner,today().minusDays(29));
        return new AnalyticsPreferences(enabled,30,events);
    }
    public AnalyticsPreferences configure(UUID owner,boolean enabled) {
        if(!lockOwner(owner)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Account not found");
        jdbc.update("INSERT INTO compass.analytics_consent(owner_id,enabled) VALUES (?,?) ON CONFLICT(owner_id) DO UPDATE SET enabled=EXCLUDED.enabled,updated_at=CURRENT_TIMESTAMP",owner,enabled);
        if(!enabled) jdbc.update("DELETE FROM compass.product_event WHERE owner_id=?",owner);
        return preferences(owner);
    }
    public void record(UUID owner,Event event) {
        // Consent withdrawal, collection and account deletion serialize on the same owner.
        if(!lockOwner(owner)) return;
        jdbc.update("DELETE FROM compass.product_event WHERE owner_id=? AND event_day<?",owner,today().minusDays(29));
        jdbc.update("INSERT INTO compass.product_event(owner_id,event_name,event_day) SELECT ?,?,? WHERE EXISTS (SELECT 1 FROM compass.analytics_consent WHERE owner_id=? AND enabled) ON CONFLICT DO NOTHING",
                owner,event.name(),today(),owner);
    }
    @Scheduled(fixedDelay=86400000,initialDelay=60000)
    public void purgeExpired() { jdbc.update("DELETE FROM compass.product_event WHERE event_day<?",today().minusDays(29)); }
}

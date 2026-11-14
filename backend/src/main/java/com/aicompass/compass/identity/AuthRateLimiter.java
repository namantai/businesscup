package com.aicompass.compass.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.sql.Timestamp;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthRateLimiter {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public AuthRateLimiter(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    // A failed authentication transaction must not roll back the attempt counter.
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public void take(String key, int limit) {
        var now = clock.instant();
        jdbc.update("DELETE FROM compass.auth_rate WHERE window_start < ?", Timestamp.from(now.minusSeconds(3600)));
        Integer attempts = jdbc.queryForObject("""
                INSERT INTO compass.auth_rate(bucket_hash,window_start,attempts) VALUES (?, ?, 1)
                ON CONFLICT(bucket_hash) DO UPDATE SET
                attempts=CASE WHEN compass.auth_rate.window_start <= ? THEN 1 ELSE compass.auth_rate.attempts+1 END,
                window_start=CASE WHEN compass.auth_rate.window_start <= ? THEN EXCLUDED.window_start ELSE compass.auth_rate.window_start END
                RETURNING attempts
                """, Integer.class, hash(key), Timestamp.from(now), Timestamp.from(now.minusSeconds(900)), Timestamp.from(now.minusSeconds(900)));
        if (attempts > limit) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
    }
}

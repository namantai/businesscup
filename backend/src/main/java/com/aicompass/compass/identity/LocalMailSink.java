package com.aicompass.compass.identity;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No network transport. Messages are secrets, outside logs and the public HTTP API. */
@Component
public class LocalMailSink {
    private final Path directory;
    private final boolean local;
    public LocalMailSink(Environment environment, @Value("${compass.identity.mail-directory:.runtime/mail}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        var profiles = java.util.Set.of(environment.getActiveProfiles());
        local = (profiles.contains("dev") || profiles.contains("test")) && !profiles.contains("prod") && !profiles.contains("staging");
    }
    public void requireLocal() {
        if (!local) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account email delivery is unavailable");
    }
    public void removeFor(String email) {
        if (!local || !Files.isDirectory(directory) || Files.isSymbolicLink(directory)) return;
        try (var files = Files.list(directory)) {
            for (var file : files.filter(p -> p.getFileName().toString().matches("[0-9a-f-]{36}\\.mail") && !Files.isSymbolicLink(p)).toList()) {
                if (Files.readString(file).startsWith("To: " + email + "\n")) Files.delete(file);
            }
        } catch (java.io.IOException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Local message cleanup unavailable");
        }
    }
    public void send(String email, String purpose, String token) {
        requireLocal();
        try {
            Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            if (Files.isSymbolicLink(directory)) throw new java.io.IOException();
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
            var file = directory.resolve(UUID.randomUUID() + ".mail");
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            // Fragment keeps the token out of HTTP request URLs/access logs/referrers.
            String route = purpose.equals("VERIFY") ? "/verify-email" : "/reset-password";
            Files.writeString(file, "To: " + email + "\nPurpose: " + purpose + "\nOpen in local frontend: " + route + "#token=" + token + "\n");
        } catch (java.io.IOException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Local email delivery unavailable");
        }
    }
}

package com.aicompass.compass.identity;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class LocalMailSinkTest {
    @TempDir Path root;

    @Test void productionStagingAndDefaultProfilesCannotWriteMessages() {
        for (String[] profiles : new String[][] {{}, {"prod"}, {"staging"}, {"dev", "prod"}, {"test", "staging"}}) {
            var environment = new MockEnvironment();
            environment.setActiveProfiles(profiles);
            var directory = root.resolve("blocked");
            var sink = new LocalMailSink(environment, directory.toString());
            var error = assertThrows(ResponseStatusException.class,
                    () -> sink.send("synthetic@example.invalid", "VERIFY", "synthetic-unused-token"));
            assertEquals(503, error.getStatusCode().value());
            assertFalse(Files.exists(directory));
        }
    }

    @Test void symlinkMailboxCannotRedirectMessageWrites() throws Exception {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        var target = Files.createDirectory(root.resolve("target"));
        var link = Files.createSymbolicLink(root.resolve("mail"), target);
        var sink = new LocalMailSink(environment, link.toString());
        var error = assertThrows(ResponseStatusException.class,
                () -> sink.send("synthetic@example.invalid", "VERIFY", "synthetic-unused-token"));
        assertEquals(503, error.getStatusCode().value());
        try (var files = Files.list(target)) { assertEquals(0, files.count()); }
    }
}

package com.aicompass.compass.catalog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Local operator CLI only. No network listener or HTTP mutation endpoints. */
@Component
@Order(100)
public class CatalogCommand implements ApplicationRunner {
    private final CatalogImportService importer;
    private final CatalogPublicationService publication;
    private final CatalogImportValidator validator;
    private final CatalogJson json;
    private final CatalogPolicy policy;
    private final JdbcTemplate jdbc;
    public CatalogCommand(CatalogImportService importer, CatalogPublicationService publication,
            CatalogImportValidator validator, CatalogJson json, CatalogPolicy policy, JdbcTemplate jdbc) {
        this.importer = importer; this.publication = publication; this.validator = validator;
        this.json = json; this.policy = policy; this.jdbc = jdbc;
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("catalog-command")) return;
        switch (one(args, "catalog-command")) {
            case "validate", "import" -> {
                Path file = Path.of(one(args, "file"));
                if (Files.size(file) > 5_000_000) throw new IllegalArgumentException("Manifest exceeds 5 MB");
                String input = Files.readString(file);
                validator.validate(json.read(input), policy.allowDemo());
                if (one(args, "catalog-command").equals("import")) {
                    UUID id = importer.importManifest(input);
                    String hash = jdbc.queryForObject("SELECT content_hash FROM compass.catalog_version WHERE id = ?", String.class, id);
                    System.out.println("IMPORTED version=" + id + " hash=" + hash);
                } else System.out.println("VALID manifest=" + json.read(input).versionId());
            }
            case "review" -> {
                publication.review(UUID.fromString(one(args, "version")), one(args, "hash"), one(args, "actor"));
                System.out.println("REVIEWED");
            }
            case "publish", "rollback" -> {
                var active = publication.activate(UUID.fromString(one(args, "version")), Long.parseLong(one(args, "expected-revision")),
                        one(args, "catalog-command").equals("rollback"), one(args, "actor"), one(args, "reason"));
                System.out.println("ACTIVE version=" + active.versionId() + " revision=" + active.revision());
            }
            case "current" -> System.out.println(publication.current());
            default -> throw new IllegalArgumentException("Unknown catalog command");
        }
    }
    private static String one(ApplicationArguments args, String name) {
        var values = args.getOptionValues(name);
        if (values == null || values.size() != 1 || values.getFirst().isBlank()) throw new IllegalArgumentException("Required option: " + name);
        return values.getFirst();
    }
}

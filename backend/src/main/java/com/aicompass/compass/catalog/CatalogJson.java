package com.aicompass.compass.catalog;

import org.springframework.stereotype.Component;
import java.time.Instant;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Component
public class CatalogJson {
    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new SimpleModule().addDeserializer(Instant.class, new UtcInstantDeserializer()))
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private static final class UtcInstantDeserializer extends ValueDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_STRING) || !parser.getString().endsWith("Z")) {
                throw new IllegalArgumentException("Catalog timestamps must be UTC ISO-8601 strings ending in Z");
            }
            return Instant.parse(parser.getString());
        }
    }

    public CatalogManifest read(String json) {
        if (json == null || json.length() > 5_000_000) throw new IllegalArgumentException("Manifest must be at most 5 MB");
        try {
            return mapper.readValue(json, CatalogManifest.class);
        } catch (RuntimeException error) {
            // Do not echo arbitrary source content into logs or reports.
            throw new IllegalArgumentException("Invalid catalog JSON: check required fields, types, enums and duplicate keys");
        }
    }

    public String write(CatalogManifest manifest) {
        return mapper.writeValueAsString(manifest);
    }
}

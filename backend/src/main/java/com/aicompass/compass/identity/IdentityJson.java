package com.aicompass.compass.identity;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

@Component
public class IdentityJson {
    private final Validator validator;
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).build();
    public IdentityJson(Validator validator) { this.validator = validator; }
    public <T> T read(String body, Class<T> type) {
        try {
            T value = mapper.readValue(body, type);
            if (value == null || !validator.validate(value).isEmpty()) throw new IllegalArgumentException();
            return value;
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request fields");
        }
    }
    public String write(Object value) { return mapper.writeValueAsString(value); }
}

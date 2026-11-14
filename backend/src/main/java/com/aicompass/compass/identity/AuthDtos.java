package com.aicompass.compass.identity;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    public record Credentials(@NotBlank @Email @Size(max=254) String email,
            @NotNull @Size(min=12,max=128) String password) {
        @Override public String toString() { return "Credentials[redacted]"; }
    }
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record TokenRequest(@NotNull @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) {
        @Override public String toString() { return "TokenRequest[redacted]"; }
    }
    public record ResetRequest(@NotNull @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
            @NotNull @Size(min=12,max=128) String password) {
        @Override public String toString() { return "ResetRequest[redacted]"; }
    }
    public record DeleteRequest(@NotNull @Size(min=1,max=128) String password, @AssertTrue boolean confirm) {
        @Override public String toString() { return "DeleteRequest[redacted]"; }
    }
    public record SessionStatus(@io.swagger.v3.oas.annotations.media.Schema(nullable=true) Account account) {}
    public record Message(String message) {}
    public record Account(UUID id, String email, String role, boolean verified, String plan, Instant createdAt) {}
}

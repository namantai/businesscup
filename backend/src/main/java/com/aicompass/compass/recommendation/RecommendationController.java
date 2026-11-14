package com.aicompass.compass.recommendation;

import com.aicompass.compass.shared.web.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class RecommendationController {
    private final RecommendationService service;
    public RecommendationController(RecommendationService service) { this.service = service; }
    public record CsrfResponse(String headerName, String token) {}
    @GetMapping("/auth/csrf")
    public CsrfResponse csrf(@io.swagger.v3.oas.annotations.Parameter(hidden = true) CsrfToken token) { return new CsrfResponse(token.getHeaderName(), token.getToken()); }
    @PostMapping(value = "/recommendations/preview", consumes = "application/json")
    @Operation(summary = "Deterministic preview without saving a profile", requestBody =
        @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content =
            @Content(schema = @Schema(implementation = RecommendationProfile.class))), responses = {
                @ApiResponse(responseCode = "200", useReturnTypeSchema = true),
                @ApiResponse(responseCode = "400", description = "Invalid profile", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = com.aicompass.compass.shared.web.ApiProblem.class))),
                @ApiResponse(responseCode = "403", description = "Missing or invalid session CSRF token", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = com.aicompass.compass.shared.web.ApiProblem.class))),
                @ApiResponse(responseCode = "413", description = "Request exceeds 64 KiB", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = com.aicompass.compass.shared.web.ApiProblem.class))),
                @ApiResponse(responseCode = "429", description = "Rate or concurrency limit; Retry-After supplied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = com.aicompass.compass.shared.web.ApiProblem.class)))})
    public RecommendationResult preview(@RequestBody String body, HttpServletRequest request) {
        return service.preview(body).withRequestId((String) request.getAttribute(RequestIdFilter.ATTRIBUTE));
    }
}

package com.aicompass.compass.config;

import com.aicompass.compass.shared.web.RequestIdFilter;
import com.aicompass.compass.shared.web.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, java.time.Clock clock) throws Exception {
        // Guest preview is read-only but uses POST; session CSRF remains mandatory.
        return http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/api/v1/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/recommendations/preview").permitAll()
                .anyRequest().denyAll())
                .addFilterBefore(new com.aicompass.compass.recommendation.PreviewLoadGuard(clock, mapper), org.springframework.security.web.csrf.CsrfFilter.class)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> forbidden(request, response, mapper))
                        .accessDeniedHandler((request, response, exception) -> forbidden(request, response, mapper)))
                .build();
    }

    private static void forbidden(HttpServletRequest request, HttpServletResponse response,
            ObjectMapper mapper) throws IOException {
        response.setStatus(403);
        response.setContentType("application/problem+json");
        mapper.writeValue(response.getOutputStream(), new ApiProblem("about:blank", "Forbidden", 403,
                "This request is not permitted.", request.getRequestURI(),
                (String) request.getAttribute(RequestIdFilter.ATTRIBUTE)));
    }


    @Bean
    UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("Accounts are not enabled"); };
    }
}

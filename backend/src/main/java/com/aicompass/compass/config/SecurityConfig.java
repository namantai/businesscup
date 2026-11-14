package com.aicompass.compass.config;

import com.aicompass.compass.identity.*;
import com.aicompass.compass.shared.web.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(); }
    @Bean UserDetailsService userDetailsService(JdbcTemplate jdbc) {
        return email -> jdbc.query("SELECT id,password_hash,role FROM compass.user_account WHERE email=?", (r,n) ->
            User.withUsername(r.getString("id")).password(r.getString("password_hash")).roles(r.getString("role")).build(),IdentityService.normalize(email))
            .stream().findFirst().orElseThrow(()->new UsernameNotFoundException("Credentials not accepted"));
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users,PasswordEncoder passwords) {
        var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(passwords);
        return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository securityContextRepository(){return new HttpSessionSecurityContextRepository();}
    @Bean CsrfTokenRepository csrfTokenRepository(){return new HttpSessionCsrfTokenRepository();}
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type=org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain securityFilterChain(HttpSecurity http,ObjectMapper mapper,Clock clock,JdbcTemplate jdbc,AuthRateLimiter rates,
            SecurityContextRepository contexts,CsrfTokenRepository csrf) throws Exception {
        return http.authorizeHttpRequests(a->a
                .requestMatchers(HttpMethod.GET,"/actuator/health","/actuator/health/**","/api/v1/catalog/**","/api/v1/billing/plans","/api/v1/openapi","/api/v1/auth/csrf","/api/v1/auth/session").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/recommendations/preview","/api/v1/auth/register","/api/v1/auth/login","/api/v1/auth/verify","/api/v1/auth/verification/resend","/api/v1/auth/password/forgot","/api/v1/auth/password/reset").permitAll()
                .requestMatchers("/api/v1/monitoring/**","/api/v1/subscriptions","/api/v1/subscriptions/**","/api/v1/me","/api/v1/me/**","/api/v1/profiles","/api/v1/profiles/**","/api/v1/recommendations","/api/v1/recommendations/*","/api/v1/saved-stacks","/api/v1/saved-stacks/**").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().denyAll())
                .securityContext(c->c.securityContextRepository(contexts))
                .csrf(c->c.csrfTokenRepository(csrf))
                .requestCache(c->c.disable())
                .logout(l->l.logoutUrl("/api/v1/auth/logout").deleteCookies("SESSION").logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
                .addFilterAfter(new IdentityGuard(jdbc,rates,mapper,clock),SecurityContextHolderFilter.class)
                .addFilterBefore(new com.aicompass.compass.recommendation.PreviewLoadGuard(clock,mapper),CsrfFilter.class)
                .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->problem(req,res,mapper,isPrivate(req)?401:403))
                    .accessDeniedHandler((req,res,ex)->problem(req,res,mapper,403)))
                .build();
    }
    private static boolean isPrivate(HttpServletRequest r){String p=r.getRequestURI();return p.startsWith("/api/v1/monitoring/")||p.startsWith("/api/v1/subscriptions")||p.startsWith("/api/v1/me")||p.startsWith("/api/v1/profiles")||p.startsWith("/api/v1/saved-stacks")||p.startsWith("/api/v1/recommendations")&&!p.endsWith("/preview");}
    private static void problem(HttpServletRequest req,HttpServletResponse res,ObjectMapper mapper,int status)throws IOException {
        res.setStatus(status);res.setContentType("application/problem+json");res.setHeader("Cache-Control","no-store");
        mapper.writeValue(res.getOutputStream(),new ApiProblem("about:blank",status==401?"Unauthorized":"Forbidden",status,"This request is not permitted.",req.getRequestURI(),(String)req.getAttribute(RequestIdFilter.ATTRIBUTE)));
    }
}

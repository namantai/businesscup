package com.aicompass.compass.identity;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.Clock;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import com.aicompass.compass.shared.web.*;

public class IdentityGuard extends OncePerRequestFilter {
    private final JdbcTemplate jdbc;
    private final AuthRateLimiter rates;
    private final ObjectMapper mapper;
    private final Clock clock;
    public IdentityGuard(JdbcTemplate jdbc, AuthRateLimiter rates, ObjectMapper mapper, Clock clock) {
        this.jdbc=jdbc;this.rates=rates;this.mapper=mapper;this.clock=clock;
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated()) {
            var session=req.getSession(false);
            var versions=jdbc.queryForList("SELECT credential_version FROM compass.user_account WHERE id=CAST(? AS uuid)",Long.class,auth.getName());
            Object version=session==null?null:session.getAttribute("credentialVersion");
            Object at=session==null?null:session.getAttribute("authenticatedAt");
            if(versions.isEmpty() || !versions.getFirst().equals(version) || !(at instanceof Long since) || clock.millis()-since>=43200000L) {
                if(session!=null)session.invalidate();
                SecurityContextHolder.clearContext();
            }
        }
        String expected=req.getHeader("X-Compass-Account");
        var current=SecurityContextHolder.getContext().getAuthentication();
        if(expected!=null && (current==null || !expected.equals(current.getName()))) {
            problem(req,res,409,"Account changed; sign in again");return;
        }
        String path=req.getRequestURI();
        boolean personal=path.startsWith("/api/v1/auth/") && !path.endsWith("/csrf") && !path.endsWith("/session")
                || path.startsWith("/api/v1/monitoring/") || path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/subscriptions") || path.startsWith("/api/v1/me") || path.startsWith("/api/v1/profiles")
                || (path.equals("/api/v1/recommendations") || path.startsWith("/api/v1/recommendations/")) && !path.endsWith("/preview")
                || path.startsWith("/api/v1/saved-stacks");
        if(!personal) {chain.doFilter(req,res);return;}
        res.setHeader("Cache-Control","no-store");
        try { rates.take("ip:"+req.getRemoteAddr(),300); }
        catch(ResponseStatusException e) { res.setHeader("Retry-After","900");problem(req,res,429,"Try again later");return; }
        if (path.startsWith("/api/v1/admin/") && (current == null || current.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))) {
            problem(req,res,403,"Administrator required"); return;
        }
        if(java.util.Set.of("POST","PATCH","DELETE").contains(req.getMethod())) {
            boolean adminDraft = path.startsWith("/api/v1/admin/catalog/drafts") && current != null
                    && current.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            int maximum = adminDraft ? 6000000 : 65536;
            byte[] body=req.getInputStream().readNBytes(maximum + 1);
            if(body.length>maximum) {problem(req,res,413,"Request too large");return;}
            var wrapped=new HttpServletRequestWrapper(req) {
                @Override public ServletInputStream getInputStream() {
                    var in=new ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        public int read(){return in.read();}
                        public boolean isFinished(){return in.available()==0;}
                        public boolean isReady(){return true;}
                        public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}
                    };
                }
                @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
            };
            chain.doFilter(wrapped,res);
        } else chain.doFilter(req,res);
    }
    private void problem(HttpServletRequest req,HttpServletResponse res,int status,String detail)throws IOException {
        res.setStatus(status);res.setContentType("application/problem+json");
        mapper.writeValue(res.getOutputStream(),new ApiProblem("about:blank",detail,status,detail,req.getRequestURI(),(String)req.getAttribute(RequestIdFilter.ATTRIBUTE)));
    }
}

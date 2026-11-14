package com.aicompass.compass.recommendation;

import com.aicompass.compass.shared.web.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Single-instance bounded limiter. Remote address only; untrusted forwarding headers never bypass it. */
public final class PreviewLoadGuard extends OncePerRequestFilter {
    private final Clock clock;
    private final ObjectMapper mapper;
    private final Map<String,Window> clients = new HashMap<>();
    private final Semaphore running = new Semaphore(2);
    private long globalStart;
    private int globalCount;
    private record Window(long start, int count) {}
    public PreviewLoadGuard(Clock clock, ObjectMapper mapper) { this.clock = clock; this.mapper = mapper; }
    synchronized boolean allow(String address) {
        long now = clock.millis();
        if (now - globalStart >= 60000) { globalStart = now; globalCount = 0; }
        clients.entrySet().removeIf(e -> now - e.getValue().start() >= 60000);
        var w = clients.get(address);
        if (globalCount >= 120 || (w == null && clients.size() >= 4096) || (w != null && w.count() >= 30)) return false;
        globalCount++; clients.put(address, new Window(w == null ? now : w.start(), w == null ? 1 : w.count() + 1));
        return true;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/api/v1/recommendations/preview") && !request.getRequestURI().equals("/api/v1/auth/csrf") && !request.getRequestURI().equals("/api/v1/subscriptions/analyze");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        if (!allow(request.getRemoteAddr()) || !running.tryAcquire()) { problem(request,response,429,"Preview rate or concurrency limit exceeded"); return; }
        try {
            if (request.getRequestURI().equals("/api/v1/recommendations/preview") && request.getMethod().equals("POST")) {
                if (request.getContentLengthLong() > 65536) { problem(request,response,413,"Profile exceeds 64 KiB"); return; }
                byte[] body = request.getInputStream().readNBytes(65537);
                if (body.length > 65536) { problem(request,response,413,"Profile exceeds 64 KiB"); return; }
                chain.doFilter(new HttpServletRequestWrapper(request) {
                    @Override public ServletInputStream getInputStream() {
                        var input = new ByteArrayInputStream(body);
                        return new ServletInputStream() {
                            @Override public int read() { return input.read(); }
                            @Override public boolean isFinished() { return input.available() == 0; }
                            @Override public boolean isReady() { return true; }
                            @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous preview only"); }
                        };
                    }
                    @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8)); }
                }, response);
            } else chain.doFilter(request, response);
        } finally { running.release(); }
    }
    private void problem(HttpServletRequest req, HttpServletResponse res, int status, String detail) throws IOException {
        res.setStatus(status); res.setContentType("application/problem+json");
        if (status == 429) res.setHeader("Retry-After", "60");
        mapper.writeValue(res.getOutputStream(), new ApiProblem("about:blank", status == 429 ? "Too Many Requests" : "Content Too Large",
                status, detail, req.getRequestURI(), (String)req.getAttribute(RequestIdFilter.ATTRIBUTE)));
    }
}

package com.jobradar.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

// Protects every request that changes data. Read-only requests (like search) stay public.
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Key";

    private static final String DEFAULT_LOCAL_KEY = "local-dev-key";
    private static final Set<String> READ_ONLY_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Logger log = LoggerFactory.getLogger(AdminApiKeyFilter.class);

    private final byte[] expectedKey;

    public AdminApiKeyFilter(@Value("${jobradar.admin.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("jobradar.admin.api-key must not be empty");
        }
        if (apiKey.equals(DEFAULT_LOCAL_KEY)) {
            log.warn("Using the default local admin key. Set ADMIN_API_KEY before deploying.");
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean readOnly = READ_ONLY_METHODS.contains(request.getMethod());
        boolean isApi = request.getRequestURI().startsWith("/api/");
        return readOnly || !isApi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER);

        // MessageDigest.isEqual compares in constant time, so the key can't be guessed by measuring response times.
        if (providedKey == null
                || !MessageDigest.isEqual(providedKey.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Missing or invalid " + HEADER + " header\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
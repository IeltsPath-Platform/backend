package com.group01.user.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class UserServiceLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String correlationId = request.getHeader("X-Correlation-Id");
        String method = request.getMethod();
        String uri = requestUri(request);

        log.info("UserService request started correlationId={} method={} path={}",
                valueOrDash(correlationId), method, uri);

        try {
            filterChain.doFilter(request, response);
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("UserService request completed correlationId={} method={} path={} status={} durationMs={}",
                    valueOrDash(correlationId), method, uri, response.getStatus(), durationMs);
        } catch (Exception exception) {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.error("UserService request failed correlationId={} method={} path={} status={} durationMs={} errorType={}",
                    valueOrDash(correlationId), method, uri, response.getStatus(), durationMs,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private String requestUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

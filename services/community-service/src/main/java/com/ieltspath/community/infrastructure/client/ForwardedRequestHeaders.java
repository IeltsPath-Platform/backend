package com.ieltspath.community.infrastructure.client;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Passes the caller's bearer token and correlation id on to another service. */
final class ForwardedRequestHeaders {
    private ForwardedRequestHeaders() {}

    static void copy(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var request = attributes.getRequest();
            var bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
            var correlation = request.getHeader("X-Correlation-Id");
            if (bearer != null) headers.set(HttpHeaders.AUTHORIZATION, bearer);
            if (correlation != null) headers.set("X-Correlation-Id", correlation);
        }
    }
}

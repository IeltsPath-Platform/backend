package com.group01.apigateway.filter;

import com.group01.commonsecurity.header.SecurityHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startedAt = System.currentTimeMillis();
        String correlationId = exchange.getAttributeOrDefault(
                CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE,
                exchange.getRequest().getHeaders().getFirst(SecurityHeaders.CORRELATION_ID)
        );
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        String requestUri = query == null ? path : path + "?" + query;

        log.info("Gateway request started correlationId={} method={} uri={}", correlationId, method, requestUri);

        return chain.filter(exchange)
                .doOnSuccess(ignored -> logCompleted(exchange, correlationId, method, requestUri, startedAt))
                .doOnError(error -> logFailed(correlationId, method, requestUri, startedAt, error));
    }

    @Override
    public int getOrder() {
        return -190;
    }

    private void logCompleted(ServerWebExchange exchange, String correlationId, String method, String requestUri, long startedAt) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode == null ? 200 : statusCode.value();
        long durationMs = System.currentTimeMillis() - startedAt;
        log.info("Gateway request completed correlationId={} method={} uri={} status={} durationMs={}",
                correlationId, method, requestUri, status, durationMs);
    }

    private void logFailed(String correlationId, String method, String requestUri, long startedAt, Throwable error) {
        long durationMs = System.currentTimeMillis() - startedAt;
        log.error("Gateway request failed correlationId={} method={} uri={} durationMs={} error={}",
                correlationId, method, requestUri, durationMs, error.getMessage(), error);
    }
}

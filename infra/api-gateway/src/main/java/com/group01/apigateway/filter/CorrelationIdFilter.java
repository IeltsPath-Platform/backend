package com.group01.apigateway.filter;

import com.group01.commonsecurity.header.SecurityHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange);
        exchange.getAttributes().put(CORRELATION_ID_ATTRIBUTE, correlationId);
        exchange.getResponse().getHeaders().set(SecurityHeaders.CORRELATION_ID, correlationId);

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(SecurityHeaders.CORRELATION_ID, correlationId))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String existingCorrelationId = exchange.getRequest().getHeaders().getFirst(SecurityHeaders.CORRELATION_ID);
        if (existingCorrelationId != null && !existingCorrelationId.isBlank()) {
            return existingCorrelationId;
        }
        return UUID.randomUUID().toString();
    }
}

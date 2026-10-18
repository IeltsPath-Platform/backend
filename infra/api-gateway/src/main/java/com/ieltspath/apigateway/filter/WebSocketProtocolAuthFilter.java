package com.ieltspath.apigateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSocketProtocolAuthFilter implements WebFilter {
    private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    private static final String NOTIFICATIONS_PATH = "/ws/notifications";
    private static final String BEARER_PREFIX = "bearer.";
    private static final String STOMP_PROTOCOL = "v12.stomp";

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        boolean notificationsPath = NOTIFICATIONS_PATH.equals(path) || path.startsWith(NOTIFICATIONS_PATH + "/");
        if (!notificationsPath || !"websocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) return chain.filter(exchange);
        List<String> protocols = request.getHeaders().getOrEmpty(SEC_WEBSOCKET_PROTOCOL).stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(","))).map(String::trim).toList();
        String token = protocols.stream().filter(value -> value.startsWith(BEARER_PREFIX) && value.length() > BEARER_PREFIX.length())
                .map(value -> value.substring(BEARER_PREFIX.length())).findFirst().orElse(null);
        if (token == null || !protocols.contains(STOMP_PROTOCOL)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        ServerHttpRequest sanitized = request.mutate().headers(headers -> {
            headers.setBearerAuth(token);
            headers.set(SEC_WEBSOCKET_PROTOCOL, STOMP_PROTOCOL);
        }).build();
        return chain.filter(exchange.mutate().request(sanitized).build());
    }
}

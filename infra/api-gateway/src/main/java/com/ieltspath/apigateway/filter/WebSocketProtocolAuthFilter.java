package com.ieltspath.apigateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSocketProtocolAuthFilter implements WebFilter {
    private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    @Override public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange, WebFilterChain chain) {
        var request=exchange.getRequest();
        if(!"/ws/notifications".equals(request.getPath().pathWithinApplication().value()) || !"websocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) return chain.filter(exchange);
        String token=request.getHeaders().getOrEmpty(SEC_WEBSOCKET_PROTOCOL).stream().flatMap(v->java.util.Arrays.stream(v.split(","))).map(String::trim).filter(v->v.startsWith("bearer.")&&v.length()>7).map(v->v.substring(7)).findFirst().orElse(null);
        boolean stomp=request.getHeaders().getOrEmpty(SEC_WEBSOCKET_PROTOCOL).stream().flatMap(v->java.util.Arrays.stream(v.split(","))).map(String::trim).anyMatch("v12.stomp"::equals);
        if(token==null||!stomp){exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);return exchange.getResponse().setComplete();}
        ServerHttpRequest sanitized=request.mutate().headers(h->{h.setBearerAuth(token);h.set(SEC_WEBSOCKET_PROTOCOL,"v12.stomp");}).build();
        return chain.filter(exchange.mutate().request(sanitized).build());
    }
}

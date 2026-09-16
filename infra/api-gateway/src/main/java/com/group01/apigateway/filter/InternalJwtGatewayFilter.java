package com.group01.apigateway.filter;

import com.group01.apigateway.security.AuthProperties;
import com.group01.apigateway.service.InternalJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InternalJwtGatewayFilter implements GlobalFilter, Ordered {
    private final InternalJwtService internalJwtService;
    private final AuthProperties authProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = withoutClientIdentity(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        Optional<String> internalJwtPath = internalJwtPath(sanitizedRequest);
        if (internalJwtPath.isEmpty()) {
            return chain.filter(sanitizedExchange);
        }

        return exchange.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    String token = internalJwtService.createToken(authentication.getToken());
                    ServerHttpRequest trustedRequest = sanitizedRequest.mutate()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
                    return chain.filter(sanitizedExchange.mutate().request(trustedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(sanitizedExchange)));
    }

    @Override
    public int getOrder() {
        return 100;
    }

    private Optional<String> internalJwtPath(ServerHttpRequest request) {
        String path = request.getPath().pathWithinApplication().value();
        return authProperties.internalJwtPaths().stream()
                .filter(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"))
                .max(Comparator.comparingInt(String::length));
    }

    private ServerHttpRequest withoutClientIdentity(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                .build();
    }
}

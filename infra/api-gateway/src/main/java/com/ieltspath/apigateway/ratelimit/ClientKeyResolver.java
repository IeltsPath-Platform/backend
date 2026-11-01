package com.ieltspath.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;

/** "user:{sub}" for authenticated callers; otherwise "ip:{address}" using X-Forwarded-For only from trusted proxies. */
public class ClientKeyResolver implements KeyResolver {
    private final RateLimitProperties properties;

    public ClientKeyResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> authentication.getToken().getSubject())
                .filter(subject -> subject != null && !subject.isBlank())
                .map(subject -> "user:" + subject)
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange)));
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null) return "unknown";
        String remoteIp = remote.getAddress() == null ? remote.getHostString() : remote.getAddress().getHostAddress();
        if (properties.trustedProxies().contains(remoteIp)) {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return Arrays.stream(forwarded.split(",", -1)).findFirst().orElse("").trim().isEmpty()
                        ? remoteIp : forwarded.split(",", -1)[0].trim();
            }
        }
        return remoteIp;
    }
}

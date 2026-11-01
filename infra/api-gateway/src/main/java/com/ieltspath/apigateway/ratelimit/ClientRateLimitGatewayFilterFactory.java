package com.ieltspath.apigateway.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/** A route filter that returns a stable error body and Retry-After when the shared Redis bucket is exhausted. */
@Component
public class ClientRateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<ClientRateLimitGatewayFilterFactory.Config> {
    private static final String RATE_LIMIT_APPLIED = ClientRateLimitGatewayFilterFactory.class.getName() + ".applied";
    private final RedisRateLimiter limiter;
    private final ClientKeyResolver keys;
    private final RateLimitProperties properties;
    private final MeterRegistry meters;

    public ClientRateLimitGatewayFilterFactory(RedisRateLimiter limiter, ClientKeyResolver keys,
                                               RateLimitProperties properties, MeterRegistry meters) {
        super(Config.class);
        this.limiter = limiter;
        this.keys = keys;
        this.properties = properties;
        this.meters = meters;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("tier");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (Boolean.TRUE.equals(exchange.getAttribute(RATE_LIMIT_APPLIED))) return chain.filter(exchange);
            String routeId = routeId(exchange);
            String tierName = resolveTier(exchange, config.getTier());
            RateLimitProperties.Tier tier = properties.tiers().getOrDefault(tierName,
                    properties.tiers().getOrDefault("default", new RateLimitProperties.Tier(20, 40)));
            exchange.getAttributes().put(RATE_LIMIT_APPLIED, Boolean.TRUE);
            return keys.resolve(exchange).flatMap(key ->
                limiter.isAllowed(tierName, key).flatMap(result -> {
                    if (result.getHeaders() == null || result.getHeaders().isEmpty()) {
                        meters.counter("rate_limit_backend_errors_total", "route", routeId, "tier", tierName).increment();
                    }
                    if (result.isAllowed()) return chain.filter(exchange);
                    meters.counter("rate_limit_rejected_total", "route", routeId, "tier", tierName).increment();
                    return reject(exchange, tier, routeId);
                }));
        };
    }

    private String resolveTier(ServerWebExchange exchange, String configuredTier) {
        String path = exchange.getRequest().getURI().getRawPath();
        if (path.equals("/auth/login") || path.equals("/auth/refresh") || path.equals("/auth/forgot-password")
                || path.equals("/api/auth/login") || path.equals("/api/auth/refresh")
                || path.equals("/api/auth/forgot-password") || path.equals("/api/users/register")) return "auth";
        if (path.equals("/api/users/me/avatar") || path.equals("/api/community/media/images")
                || path.equals("/api/content/admin/media/audio") || path.equals("/api/content/admin/media/image")
                || path.equals("/api/assessments/speaking-recordings")
                || path.equals("/api/learning-support/flashcards/images")) return "upload";
        if (path.startsWith("/api/content/admin/ai-drafts/")
                || path.startsWith("/api/assessments/admin/ai-")
                || path.matches("/api/assessments/attempts/[^/]+/submit")) return "ai";
        return configuredTier;
    }

    private Mono<Void> reject(ServerWebExchange exchange, RateLimitProperties.Tier tier, String routeId) {
        int retryAfter = Math.max(1, (int) Math.ceil(1.0 / tier.replenishRate()));
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set("Retry-After", Integer.toString(retryAfter));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,"
                + "\"error\":\"Too Many Requests\",\"message\":\"Request rate limit exceeded\","
                + "\"path\":\"" + escape(exchange.getRequest().getURI().getRawPath()) + "\","
                + "\"code\":\"RATE_LIMITED\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String routeId(ServerWebExchange exchange) {
        Object route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route instanceof org.springframework.cloud.gateway.route.Route gatewayRoute
                ? gatewayRoute.getId() : "unknown-route";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class Config {
        private String tier = "default";

        public String getTier() { return tier; }
        public void setTier(String tier) { this.tier = tier; }
    }
}

package com.ieltspath.apigateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties("gateway.rate-limit")
public record RateLimitProperties(List<String> trustedProxies, Map<String, Tier> tiers) {
    public RateLimitProperties {
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
        tiers = tiers == null ? Map.of() : Map.copyOf(tiers);
    }

    public record Tier(int replenishRate, int burstCapacity) {
        public Tier {
            if (replenishRate < 1 || burstCapacity < 1) {
                throw new IllegalArgumentException("Rate limit replenishRate and burstCapacity must be positive");
            }
        }
    }
}

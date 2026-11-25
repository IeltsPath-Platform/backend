package com.ieltspath.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {
    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);
    @Bean
    ClientKeyResolver clientKeyResolver(RateLimitProperties properties) {
        return new ClientKeyResolver(properties);
    }

    @Bean
    RedisRateLimiter redisRateLimiter(RateLimitProperties properties) {
        RedisRateLimiter limiter = new RedisRateLimiter(1, 5);
        Map<String, RedisRateLimiter.Config> tiers = new HashMap<>();
        properties.tiers().forEach((name, tier) -> tiers.put(name, new RedisRateLimiter.Config()
                .setReplenishRate(tier.replenishRate())
                .setBurstCapacity(tier.burstCapacity())));
        limiter.getConfig().putAll(tiers);
        return limiter;
    }

    @Bean
    ApplicationRunner trustedProxyConfigurationWarning(RateLimitProperties properties) {
        return arguments -> {
            if (properties.behindProxy() && properties.trustedProxies().isEmpty()) {
                log.warn("Gateway runs behind a proxy but gateway.rate-limit.trusted-proxies is empty; anonymous clients share the proxy rate-limit bucket");
            }
        };
    }
}

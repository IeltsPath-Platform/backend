package com.ieltspath.apigateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {
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
}

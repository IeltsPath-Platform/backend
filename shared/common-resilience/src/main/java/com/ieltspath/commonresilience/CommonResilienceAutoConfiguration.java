package com.ieltspath.commonresilience;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonResilienceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    ResilientCall resilientCall(io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry breakers,
                                io.github.resilience4j.retry.RetryRegistry retries,
                                io.github.resilience4j.bulkhead.BulkheadRegistry bulkheads) {
        return new ResilientCall(breakers, retries, bulkheads);
    }
}

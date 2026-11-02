package com.ieltspath.commonobservability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Registers the correlation id filter and the outgoing REST customizer for every servlet service that adds this module. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonObservabilityAutoConfiguration {
    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    CorrelationRestClientCustomizer correlationRestClientCustomizer() {
        return new CorrelationRestClientCustomizer();
    }
}

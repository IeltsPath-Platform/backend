package com.ieltspath.commonobservability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

/**
 * Carries the correlation id over RabbitMQ. Kept apart from the main configuration and conditional on spring-rabbit, so a
 * service that does not use RabbitMQ loads no Rabbit class and gets no broker connection from this module.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
public class CorrelationRabbitAutoConfiguration {
    @Bean
    static CorrelationRabbitBeanPostProcessor correlationRabbitBeanPostProcessor() {
        return new CorrelationRabbitBeanPostProcessor();
    }

    @Bean
    OutboxTraceContext outboxTraceContext(ObjectProvider<Tracer> tracer, ObjectProvider<Propagator> propagator) {
        return new OutboxTraceContext(tracer, propagator);
    }
}

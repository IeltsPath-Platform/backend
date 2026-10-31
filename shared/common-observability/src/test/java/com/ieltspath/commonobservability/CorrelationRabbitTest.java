package com.ieltspath.commonobservability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

class CorrelationRabbitTest {
    private final CorrelationRabbitBeanPostProcessor processor = new CorrelationRabbitBeanPostProcessor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void publishedMessageCarriesTheCurrentCorrelationId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "req-42");
        Message message = MessageBuilder.withBody(new byte[0]).build();

        CorrelationRabbitBeanPostProcessor.stamp(message);

        assertEquals("req-42", message.getMessageProperties().getHeader(CorrelationRabbitBeanPostProcessor.HEADER));
    }

    @Test
    void publishOutsideARequestAddsNoHeader() {
        Message message = MessageBuilder.withBody(new byte[0]).build();

        CorrelationRabbitBeanPostProcessor.stamp(message);

        assertNull(message.getMessageProperties().getHeader(CorrelationRabbitBeanPostProcessor.HEADER));
    }

    @Test
    void listenerRunsWithTheIdOfTheMessageItReceived() throws Throwable {
        Message message = MessageBuilder.withBody(new byte[0]).build();
        message.getMessageProperties().setHeader(CorrelationRabbitBeanPostProcessor.HEADER, "evt-7");
        String[] seen = new String[1];
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[] {message});
        when(invocation.proceed()).thenAnswer(call -> {
            seen[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
            return null;
        });

        new CorrelationListenerAdvice().invoke(invocation);

        assertEquals("evt-7", seen[0]);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void listenerWithoutAnIdGetsAFreshOneAndNeverKeepsIt() throws Throwable {
        Message message = MessageBuilder.withBody(new byte[0]).build();
        String[] seen = new String[1];
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[] {message});
        when(invocation.proceed()).thenAnswer(call -> {
            seen[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
            return null;
        });

        new CorrelationListenerAdvice().invoke(invocation);

        assertNotEquals(null, seen[0]);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void listenerFailureStillRestoresTheContext() throws Throwable {
        Message message = MessageBuilder.withBody(new byte[0]).build();
        message.getMessageProperties().setHeader(CorrelationRabbitBeanPostProcessor.HEADER, "evt-8");
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[] {message});
        when(invocation.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> new CorrelationListenerAdvice().invoke(invocation));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void everyListenerFactoryGetsTheCorrelationAdviceOnce() {
        var factory = new SimpleRabbitListenerContainerFactory();

        processor.postProcessAfterInitialization(factory, "gradingFactory");
        processor.postProcessAfterInitialization(factory, "gradingFactory");

        long advised = java.util.Arrays.stream(factory.getAdviceChain())
                .filter(advice -> advice instanceof CorrelationListenerAdvice).count();
        assertEquals(1, advised);
    }

    @Test
    void everyRabbitTemplateStampsItsMessages() {
        var template = new RabbitTemplate(new CachingConnectionFactory("localhost"));

        processor.postProcessAfterInitialization(template, "rabbit");

        assertEquals(1, template.getBeforePublishPostProcessors().size());
    }
}

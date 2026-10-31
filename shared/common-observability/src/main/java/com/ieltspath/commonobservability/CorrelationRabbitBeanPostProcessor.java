package com.ieltspath.commonobservability;

import java.util.Arrays;
import org.aopalliance.aop.Advice;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Carries the correlation id across RabbitMQ: every template stamps the id of the current request onto the message, and
 * every listener container runs its listener with the id of the message it received.
 */
public class CorrelationRabbitBeanPostProcessor implements BeanPostProcessor {
    public static final String HEADER = "x-correlation-id";
    private static final MessagePostProcessor STAMP = CorrelationRabbitBeanPostProcessor::stamp;

    static Message stamp(Message message) {
        String id = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (id != null) {
            message.getMessageProperties().setHeader(HEADER, id);
        }
        return message;
    }

    /** The template reports null, not an empty list, when no post processor was ever added. */
    private static boolean alreadyStamps(RabbitTemplate template) {
        var current = template.getBeforePublishPostProcessors();
        return current != null && current.contains(STAMP);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof RabbitTemplate template && !alreadyStamps(template)) {
            template.addBeforePublishPostProcessors(STAMP);
        }
        if (bean instanceof SimpleRabbitListenerContainerFactory factory) {
            Advice[] chain = factory.getAdviceChain() == null ? new Advice[0] : factory.getAdviceChain();
            boolean advised = Arrays.stream(chain).anyMatch(CorrelationListenerAdvice.class::isInstance);
            if (!advised) {
                Advice[] extended = Arrays.copyOf(chain, chain.length + 1);
                extended[chain.length] = new CorrelationListenerAdvice();
                factory.setAdviceChain(extended);
            }
        }
        return bean;
    }
}

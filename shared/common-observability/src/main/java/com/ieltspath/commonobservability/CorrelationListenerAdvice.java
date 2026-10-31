package com.ieltspath.commonobservability;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import java.util.UUID;

/**
 * Runs a listener with the correlation id of the message it received, and restores the previous logging context after
 * the listener returns or fails. A message without an id gets a fresh one, so its log lines still group together.
 */
public class CorrelationListenerAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String previous = MDC.get(CorrelationIdFilter.MDC_KEY);
        MDC.put(CorrelationIdFilter.MDC_KEY, idOf(invocation.getArguments()));
        try {
            return invocation.proceed();
        } finally {
            if (previous == null) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            } else {
                MDC.put(CorrelationIdFilter.MDC_KEY, previous);
            }
        }
    }

    private static String idOf(Object[] arguments) {
        if (arguments != null && arguments.length > 0 && arguments[0] instanceof Message message) {
            Object header = message.getMessageProperties().getHeader(CorrelationRabbitBeanPostProcessor.HEADER);
            if (header != null) {
                return CorrelationIdFilter.safeOrNew(header.toString());
            }
        }
        return UUID.randomUUID().toString();
    }
}

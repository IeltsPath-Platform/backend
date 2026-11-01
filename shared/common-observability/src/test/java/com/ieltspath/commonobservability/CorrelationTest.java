package com.ieltspath.commonobservability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationTest {
    @AfterEach
    void clearMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    void currentReturnsMdcValue() throws Exception {
        MDC.put(CorrelationIdFilter.MDC_KEY, "req-1");

        assertEquals("req-1", current());
    }

    @Test
    void currentIsNullOutsideRequest() throws Exception {
        assertNull(current());
    }

    private static String current() throws Exception {
        try {
            return (String) Class.forName("com.ieltspath.commonobservability.Correlation")
                    .getMethod("current").invoke(null);
        } catch (InvocationTargetException failure) {
            throw new AssertionError(failure.getCause());
        }
    }
}

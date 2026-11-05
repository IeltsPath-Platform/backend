package com.ieltspath.commonobservability;

import org.slf4j.MDC;

/** Access to the current request correlation id for adapters that persist asynchronous work. */
public final class Correlation {
    public static final String HEADER = "x-correlation-id";

    private Correlation() { }

    public static String current() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}

package com.ieltspath.commonobservability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.util.LogbackMDCAdapter;

import java.nio.charset.StandardCharsets;

/**
 * Checks the level pattern and the JSON encoder against a private Logback context. The test does not start Spring, so
 * the result does not depend on which logging configuration an earlier test in the same JVM installed.
 */
class LogFormatTest {
    private static final String LEVEL_PATTERN = "%5p [%X{correlationId:-}]";

    private final LoggerContext context = new LoggerContext();
    private final Logger logger = context.getLogger(LogFormatTest.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();

    @BeforeEach
    void capture() {
        context.setMDCAdapter(new LogbackMDCAdapter());
        events.setContext(context);
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void release() {
        logger.detachAppender(events);
        events.stop();
        context.getMDCAdapter().clear();
    }

    @Test
    void textPatternKeepsTheCorrelationIdInTheLevelColumn() {
        context.getMDCAdapter().put(CorrelationIdFilter.MDC_KEY, "text-req-2");
        logger.info("handled a request");

        String line = render(LEVEL_PATTERN + " %msg", events.list.get(0));

        assertTrue(line.contains("INFO [text-req-2]"), line);
        assertFalse(line.contains("\"correlationId\""), line);
    }

    @Test
    void jsonEncoderWritesTheCorrelationIdAsAField() {
        context.getMDCAdapter().put(CorrelationIdFilter.MDC_KEY, "json-req-1");
        logger.info("handled a request");

        String json = encodeJson(events.list.get(0));

        assertTrue(json.contains("\"correlationId\":\"json-req-1\""), json);
    }

    private String render(String pattern, ILoggingEvent event) {
        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern(pattern);
        layout.start();
        try {
            return layout.doLayout(event);
        } finally {
            layout.stop();
        }
    }

    private String encodeJson(ILoggingEvent event) {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);
        encoder.start();
        try {
            return new String(encoder.encode(event), StandardCharsets.UTF_8);
        } finally {
            encoder.stop();
        }
    }
}
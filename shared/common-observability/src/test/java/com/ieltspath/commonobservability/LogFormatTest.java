package com.ieltspath.commonobservability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@ExtendWith(OutputCaptureExtension.class)
class LogFormatTest {
    private static final String LEVEL_PATTERN = "%5p [%X{correlationId:-}]";

    @Test
    void jsonProfileWritesTheCorrelationIdAsAField(CapturedOutput output) {
        logWithId("json-req-1", "json-logs", output);

        assertTrue(output.getAll().contains("\"correlationId\":\"json-req-1\""), output.getAll());
    }

    @Test
    void defaultProfileKeepsTheCorrelationIdInTheLevelColumn(CapturedOutput output) {
        logWithId("text-req-2", null, output);

        assertTrue(output.getAll().contains("INFO [text-req-2]"), output.getAll());
        assertFalse(output.getAll().contains("\"correlationId\""), output.getAll());
    }

    private static void logWithId(String id, String profile, CapturedOutput output) {
        var builder = new SpringApplicationBuilder(EmptyContext.class)
                .web(WebApplicationType.NONE)
                .properties("logging.pattern.level=" + LEVEL_PATTERN);
        if (profile != null) {
            builder.profiles(profile);
        }
        try (ConfigurableApplicationContext ignored = builder.run()) {
            MDC.put(CorrelationIdFilter.MDC_KEY, id);
            try {
                LoggerFactory.getLogger(LogFormatTest.class).info("handled a request");
            } finally {
                MDC.clear();
            }
        }
    }

    @Configuration
    static class EmptyContext {
    }
}

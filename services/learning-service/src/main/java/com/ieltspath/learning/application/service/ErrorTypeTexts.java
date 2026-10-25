package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/** Best-effort lookup of learner-facing text for error types. */
@Service
public class ErrorTypeTexts {
    private static final Logger log = LoggerFactory.getLogger(ErrorTypeTexts.class);
    private final LearningContentClient content;

    public ErrorTypeTexts(LearningContentClient content) {
        this.content = content;
    }

    public Map<String, Text> byCode() {
        try {
            var errorTypes = content.learnerErrorTypes();
            if (errorTypes == null) return Map.of();
            return errorTypes.stream().filter(type -> type != null && type.code() != null)
                    .collect(Collectors.toMap(LearningContentClient.ErrorType::code,
                            type -> new Text(type.name(), type.fixHint()), (first, ignored) -> first));
        } catch (RuntimeException exception) {
            log.warn("Unable to load active error type text: {}", exception.getMessage());
            return Map.of();
        }
    }

    public record Text(String name, String fixHint) {}
}

package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads the Speaking error types and knowledge points from Content while a learner request is running, and turns the
 * stored copy back into a {@link SpeakingGradingContext} in the grading job. A Content outage here only costs the
 * personalised part of the feedback, so it is logged and the context is left empty; the grade does not wait on it.
 */
@Component
public class SpeakingGradingContextCapture {
    private static final Logger log = LoggerFactory.getLogger(SpeakingGradingContextCapture.class);
    private static final String ERROR_CODE_PREFIX = "SPK_";

    private final LearningContentClient content;
    private final SpeakingHistorySummarizer history;
    private final ObjectMapper json;

    public SpeakingGradingContextCapture(LearningContentClient content, SpeakingHistorySummarizer history,
                                         ObjectMapper json) {
        this.content = content;
        this.history = history;
        this.json = json;
    }

    /** Must run inside a learner request: the Content calls forward the learner's token. */
    public String capture() {
        List<SpeakingGradingInput.ErrorOption> errorTypes = List.of();
        try {
            errorTypes = content.activeErrorTypes().stream()
                    .filter(type -> type.code() != null && type.code().startsWith(ERROR_CODE_PREFIX))
                    .map(type -> new SpeakingGradingInput.ErrorOption(type.code(), type.name())).toList();
        } catch (RuntimeException unavailable) {
            log.warn("Speaking error types unavailable: {}", unavailable.getClass().getSimpleName());
        }
        List<LearningContentClient.KnowledgePoint> points = List.of();
        try {
            points = history.relevantPoints(content.getTopicSequence());
        } catch (RuntimeException unavailable) {
            log.warn("Speaking knowledge points unavailable: {}", unavailable.getClass().getSimpleName());
        }
        try {
            return json.writeValueAsString(new SpeakingGradingContext(errorTypes, points));
        } catch (Exception notSerializable) {
            throw new IllegalStateException("The Speaking grading context is not serializable", notSerializable);
        }
    }

    /** An absent or unreadable copy reads as empty: the grade is then given without error codes or personal advice. */
    public SpeakingGradingContext read(String stored) {
        if (stored == null || stored.isBlank()) return SpeakingGradingContext.empty();
        try {
            return json.readValue(stored, SpeakingGradingContext.class);
        } catch (Exception unreadable) {
            log.warn("Stored Speaking grading context is unreadable: {}", unreadable.getClass().getSimpleName());
            return SpeakingGradingContext.empty();
        }
    }
}

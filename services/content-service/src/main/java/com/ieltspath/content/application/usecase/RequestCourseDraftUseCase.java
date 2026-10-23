package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.UUID;

/** Validates an author's request for a course draft, then queues it against the author's allowance. */
@Service
@Transactional
public class RequestCourseDraftUseCase {

    static final int MAX_TOPICS = 10;
    static final int MAX_TOPIC_LENGTH = 100;
    static final int MAX_LESSONS = 30;
    static final int MAX_INSTRUCTIONS = 500;

    private final StartAiDraftUseCase start;
    private final ObjectMapper json;
    private final ContentAiProperties properties;

    public RequestCourseDraftUseCase(StartAiDraftUseCase start, ObjectMapper json, ContentAiProperties properties) {
        this.start = start;
        this.json = json;
        this.properties = properties;
    }

    public AiDraft execute(UUID author, CourseDraftRequest request) {
        validate(request);
        return start.execute(author, DraftKind.COURSE, body(request), properties.getAi().getDailyLimit());
    }

    private void validate(CourseDraftRequest request) {
        BigDecimal band = request.bandLevel();
        if (band == null || band.signum() < 0 || band.compareTo(new BigDecimal("9")) > 0
                || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("bandLevel must be a half band from 0 to 9");
        }
        if (request.skills().isEmpty() || new HashSet<>(request.skills()).size() != request.skills().size()) {
            throw new IllegalArgumentException("skills must name each skill at most once and at least one");
        }
        if (request.topics().isEmpty() && request.knowledgePointIds().isEmpty()) {
            throw new IllegalArgumentException("topics or knowledgePointIds is required");
        }
        if (request.topics().size() > MAX_TOPICS || request.topics().stream()
                .anyMatch(topic -> topic.isBlank() || topic.length() > MAX_TOPIC_LENGTH)) {
            throw new IllegalArgumentException("topics must be at most " + MAX_TOPICS + " phrases of up to "
                    + MAX_TOPIC_LENGTH + " characters");
        }
        int maxPoints = properties.getAi().getMaxCourseCandidates();
        if (request.knowledgePointIds().size() > maxPoints) {
            throw new IllegalArgumentException("knowledgePointIds must not exceed " + maxPoints);
        }
        if (request.lessonCount() != null && (request.lessonCount() < 1 || request.lessonCount() > MAX_LESSONS)) {
            throw new IllegalArgumentException("lessonCount must be between 1 and " + MAX_LESSONS);
        }
        if (request.instructions() != null && request.instructions().length() > MAX_INSTRUCTIONS) {
            throw new IllegalArgumentException("instructions must not exceed " + MAX_INSTRUCTIONS + " characters");
        }
    }

    private String body(CourseDraftRequest request) {
        try {
            return json.writeValueAsString(request);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Request could not be serialized", invalid);
        }
    }
}

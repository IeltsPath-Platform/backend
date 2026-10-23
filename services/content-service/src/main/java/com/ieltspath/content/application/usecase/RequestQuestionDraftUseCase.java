package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.service.QuestionDraftValidator;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Validates an author's request for question drafts, then queues it against the author's allowance. */
@Service
@Transactional
public class RequestQuestionDraftUseCase {

    static final int MAX_INSTRUCTIONS = 500;

    private final KnowledgePointRepository knowledgePoints;
    private final StartAiDraftUseCase start;
    private final ObjectMapper json;
    private final ContentAiProperties properties;

    public RequestQuestionDraftUseCase(KnowledgePointRepository knowledgePoints, StartAiDraftUseCase start,
                                       ObjectMapper json, ContentAiProperties properties) {
        this.knowledgePoints = knowledgePoints;
        this.start = start;
        this.json = json;
        this.properties = properties;
    }

    public AiDraft execute(UUID author, QuestionDraftRequest request) {
        KnowledgePoint point = knowledgePoints.findById(request.knowledgePointId())
                .orElseThrow(() -> new KnowledgePointNotFoundException(request.knowledgePointId()));
        if (point.getStatus() != ContentStatus.ACTIVE) {
            throw new InvalidContentStateException("Knowledge point is not active");
        }
        if (request.questionType() == null || !QuestionDraftValidator.supports(request.questionType())) {
            throw new IllegalArgumentException("questionType is not supported for AI drafts");
        }
        if (request.difficulty() == null) {
            throw new IllegalArgumentException("difficulty is required");
        }
        int maxItems = properties.getAi().getMaxItemsPerRequest();
        if (request.count() < 1 || request.count() > maxItems) {
            throw new IllegalArgumentException("count must be between 1 and " + maxItems);
        }
        if (request.instructions() != null && request.instructions().length() > MAX_INSTRUCTIONS) {
            throw new IllegalArgumentException("instructions must not exceed " + MAX_INSTRUCTIONS + " characters");
        }
        return start.execute(author, DraftKind.QUESTIONS, body(request), properties.getAi().getDailyLimit());
    }

    private String body(QuestionDraftRequest request) {
        try {
            return json.writeValueAsString(request);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Request could not be serialized", invalid);
        }
    }
}

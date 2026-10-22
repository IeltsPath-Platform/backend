package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.result.QuestionDetailResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.QuestionDifficultyRequiredForPublicationException;
import com.ieltspath.content.domain.exception.QuestionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionNotPublishableException;
import com.ieltspath.content.domain.exception.QuestionVersionStateException;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class PublishQuestionVersionUseCase {
    private final QuestionRepository questions;
    private final ObjectMapper objectMapper;

    public PublishQuestionVersionUseCase(QuestionRepository questions, ObjectMapper objectMapper) {
        this.questions = questions;
        this.objectMapper = objectMapper;
    }

    public QuestionDetailResult execute(UUID questionId, UUID versionId) {
        Question question = questions.findByIdForUpdate(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        QuestionVersion version = question.getVersions().stream().filter(v -> v.getId().equals(versionId))
                .findFirst().orElseThrow(() -> new QuestionVersionNotFoundException(versionId));
        if (version.getStatus() != PublicationStatus.DRAFT) {
            throw new QuestionVersionStateException("Only draft question versions can be published");
        }
        if (version.getDifficulty() == null) throw new QuestionDifficultyRequiredForPublicationException();
        validate(question.getQuestionType(), version);
        question.publishVersion(versionId);
        return QuestionDetailResult.from(questions.save(question));
    }

    private void validate(QuestionType questionType, QuestionVersion version) {
        try {
            JsonNode spec = objectMapper.readTree(version.getAnswerSpecJson());
            if (spec == null || !spec.isObject()) throw new QuestionVersionNotPublishableException();
            String type = spec.path("type").asText("CHOICE");
            if (questionType == QuestionType.ESSAY || "ESSAY".equals(type)) {
                validateEssay(questionType, spec);
            } else if ("CHOICE".equals(type)) {
                JsonNode correct = spec.path("correct");
                if (!correct.isTextual() || correct.asText().isBlank()) throw new QuestionVersionNotPublishableException();
                if (version.getOptions().stream().noneMatch(option -> option.optionKey().equals(correct.asText()))) {
                    throw new QuestionVersionNotPublishableException();
                }
            } else if ("FILL".equals(type)) {
                JsonNode accepted = spec.path("accepted");
                if (!accepted.isArray() || accepted.isEmpty()) throw new QuestionVersionNotPublishableException();
                for (JsonNode answer : accepted) {
                    if (!answer.isTextual() || answer.asText().isBlank()) throw new QuestionVersionNotPublishableException();
                }
            } else {
                throw new QuestionVersionNotPublishableException();
            }
        } catch (QuestionVersionNotPublishableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QuestionVersionNotPublishableException();
        }
    }

    private static void validateEssay(QuestionType questionType, JsonNode spec) {
        if (questionType != QuestionType.ESSAY || !"ESSAY".equals(spec.path("type").asText())) {
            throw new QuestionVersionNotPublishableException();
        }
        String task = spec.path("task").asText();
        JsonNode passBand = spec.path("passBand");
        if (!("TASK_1".equals(task) || "TASK_2".equals(task)) || !passBand.isNumber()) {
            throw new QuestionVersionNotPublishableException();
        }
        BigDecimal band = passBand.decimalValue();
        if (band.compareTo(BigDecimal.valueOf(4)) < 0 || band.compareTo(BigDecimal.valueOf(9)) > 0
                || band.remainder(new BigDecimal("0.5")).signum() != 0) {
            throw new QuestionVersionNotPublishableException();
        }
        if ("TASK_1".equals(task)) {
            String facts = spec.path("chartFacts").asText();
            if (facts.length() < 1 || facts.length() > 2000) throw new QuestionVersionNotPublishableException();
        }
    }
}

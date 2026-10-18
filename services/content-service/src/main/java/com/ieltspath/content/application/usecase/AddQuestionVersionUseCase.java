package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.entity.QuestionKnowledgePoint;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.DifficultyRequiredException;
import com.ieltspath.content.domain.exception.InvalidErrorTypeException;
import com.ieltspath.content.domain.exception.QuestionNotFoundException;
import com.ieltspath.content.domain.exception.UnknownErrorCodeException;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddQuestionVersionUseCase {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final QuestionRepository questionRepository;
    private final ErrorTypeRepository errorTypeRepository;

    public AddQuestionVersionUseCase(QuestionRepository questionRepository, ErrorTypeRepository errorTypeRepository) {
        this.questionRepository = questionRepository;
        this.errorTypeRepository = errorTypeRepository;
    }

    public QuestionResult execute(AddQuestionVersionCommand command) {
        if (command.difficulty() == null) {
            throw new DifficultyRequiredException();
        }
        Question question = questionRepository.findById(command.questionId())
                .orElseThrow(() -> new QuestionNotFoundException(command.questionId()));
        validateDistractors(command.answerSpecJson(), command.options());

        QuestionVersion version = QuestionVersion.create(
                command.questionId(),
                command.versionNumber(),
                command.stem(),
                command.options(),
                command.answerSpecJson(),
                command.explanation(),
                command.hint(),
                command.difficulty()
        );

        if (command.knowledgePoints() != null) {
            for (var knowledgePoint : command.knowledgePoints()) {
                // The version id is generated above, so a mapping always belongs to this version;
                // a client-supplied questionVersionId is ignored.
                version.addKnowledgePoint(new QuestionKnowledgePoint(
                        version.getId(),
                        knowledgePoint.knowledgePointId(),
                        knowledgePoint.weight()
                ));
            }
        }

        question.addVersion(version);
        Question saved = questionRepository.save(question);

        return new QuestionResult(
                saved.getId(),
                saved.getQuestionType(),
                saved.getSkill(),
                saved.getRequiredFeatureKey(),
                saved.getStatus(),
                saved.getCurrentPublishedVersionId(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                saved.getPurpose()
        );
    }

    /**
     * A {@code distractors} map ties a wrong option key to an error code. Each key must be an option of this question
     * and each code must be an active error type. Only CHOICE questions have options, so a FILL spec cannot carry one.
     */
    private void validateDistractors(String answerSpecJson, List<QuestionOptionPayload> options) {
        JsonNode spec = parse(answerSpecJson);
        JsonNode distractors = spec == null ? null : spec.get("distractors");
        if (distractors == null || distractors.isNull()) {
            return;
        }
        if (!distractors.isObject()) {
            throw new InvalidErrorTypeException("distractors must be an object of option key to error code");
        }
        Set<String> optionKeys = options == null ? Set.of()
                : options.stream().map(QuestionOptionPayload::optionKey).collect(Collectors.toSet());
        Set<String> codes = new LinkedHashSet<>();
        for (Map.Entry<String, JsonNode> entry : distractors.properties()) {
            if (!optionKeys.contains(entry.getKey())) {
                throw new InvalidErrorTypeException("distractor key " + entry.getKey() + " is not an option of this question");
            }
            if (!entry.getValue().isTextual()) {
                throw new InvalidErrorTypeException("distractor for option " + entry.getKey() + " must be an error code");
            }
            codes.add(entry.getValue().asText());
        }
        Set<String> active = errorTypeRepository.findActiveCodes(codes);
        List<String> unknown = codes.stream().filter(code -> !active.contains(code)).toList();
        if (!unknown.isEmpty()) {
            throw new UnknownErrorCodeException(unknown);
        }
    }

    private static JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readTree(json);
        } catch (Exception exception) {
            return null;
        }
    }
}

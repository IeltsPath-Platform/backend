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
import com.ieltspath.content.domain.service.TrueFalseNotGivenTraps;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import com.ieltspath.content.domain.vo.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        String answerSpecJson = addTfngDistractorsIfAvailable(question, command.answerSpecJson(), command.options());
        validateDistractors(answerSpecJson, command.options());

        if (command.knowledgePoints() != null) {
            Set<java.util.UUID> mappedPointIds = command.knowledgePoints().stream()
                    .map(AddQuestionVersionCommand.KnowledgePointInput::knowledgePointId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (errorTypeRepository.containsTrapKnowledgePoints(mappedPointIds)) {
                throw new InvalidErrorTypeException("TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE");
            }
        }

        QuestionVersion version = QuestionVersion.create(
                command.questionId(),
                command.versionNumber(),
                command.stem(),
                command.options(),
                answerSpecJson,
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

        return QuestionResult.of(saved);
    }

    private String addTfngDistractorsIfAvailable(Question question, String answerSpecJson,
                                                  List<QuestionOptionPayload> options) {
        if (question.getQuestionType() != QuestionType.TRUE_FALSE_NOT_GIVEN) {
            return answerSpecJson;
        }
        JsonNode parsed = parse(answerSpecJson);
        if (parsed == null || !parsed.isObject() || parsed.has("distractors") || options == null) {
            return answerSpecJson;
        }
        Set<String> requiredCodes = Set.of("TFNG_NG_AS_FALSE", "TFNG_FALSE_AS_NG");
        if (!errorTypeRepository.findActiveCodes(requiredCodes).containsAll(requiredCodes)) {
            return answerSpecJson;
        }
        Set<String> optionKeys = options.stream().map(QuestionOptionPayload::optionKey).collect(Collectors.toSet());
        Map<String, String> distractors = TrueFalseNotGivenTraps.distractorsFor(
                parsed.path("correct").isTextual() ? parsed.path("correct").asText() : null, optionKeys);
        if (distractors.isEmpty()) {
            return answerSpecJson;
        }
        ObjectNode updated = ((ObjectNode) parsed).deepCopy();
        ObjectNode mappings = JSON.createObjectNode();
        distractors.forEach(mappings::put);
        updated.set("distractors", mappings);
        return updated.toString();
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

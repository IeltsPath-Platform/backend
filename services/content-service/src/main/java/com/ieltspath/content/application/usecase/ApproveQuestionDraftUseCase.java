package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.command.QuestionDraftRequest;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Turns the chosen items of a ready draft into ordinary DRAFT questions, through the same use cases every other
 * question goes through. All or nothing: one item that fails leaves the draft READY and creates nothing.
 */
@Service
@Transactional
public class ApproveQuestionDraftUseCase {

    private final AiDraftRepository drafts;
    private final CreateQuestionUseCase createQuestion;
    private final AddQuestionVersionUseCase addVersion;
    private final ObjectMapper json;
    private final Clock clock;

    public ApproveQuestionDraftUseCase(AiDraftRepository drafts, CreateQuestionUseCase createQuestion,
                                       AddQuestionVersionUseCase addVersion, ObjectMapper json, Clock clock) {
        this.drafts = drafts;
        this.createQuestion = createQuestion;
        this.addVersion = addVersion;
        this.json = json;
        this.clock = clock;
    }

    public AiDraft execute(UUID reviewer, UUID draftId, List<Integer> itemIndexes) {
        AiDraft draft = drafts.findById(draftId)
                .filter(found -> found.getRequestedBy().equals(reviewer))
                .orElseThrow(() -> new AiDraftNotFoundException(draftId));
        if (draft.getKind() != DraftKind.QUESTIONS) {
            throw new InvalidContentStateException("Draft is not a question draft");
        }
        if (draft.getStatus() != DraftStatus.READY) {
            throw new InvalidContentStateException("Draft is " + draft.getStatus() + ", expected READY");
        }
        if (itemIndexes == null || itemIndexes.isEmpty()) {
            throw new IllegalArgumentException("itemIndexes must name at least one item");
        }
        JsonNode items = read(draft.getResultJson()).path("items");
        TreeSet<Integer> chosen = new TreeSet<>(itemIndexes);
        if (chosen.first() < 0 || chosen.last() >= items.size()) {
            throw new IllegalArgumentException("itemIndexes refer to an item that does not exist");
        }
        QuestionDraftRequest request = readRequest(draft);

        draft.approve(reviewer, clock.instant());
        for (int index : chosen) {
            createFrom(items.get(index), request, draftId, index);
        }
        return drafts.save(draft);
    }

    private void createFrom(JsonNode item, QuestionDraftRequest request, UUID draftId, int index) {
        QuestionResult question = createQuestion.execute(new CreateQuestionCommand(
                request.questionType(), null, null, QuestionPurpose.LEARNING, "ai-" + draftId + "-" + index));
        addVersion.execute(new AddQuestionVersionCommand(
                question.id(), 1, item.path("stem").asText(), options(item), item.path("answerSpec").toString(),
                item.path("explanation").asText(), null, request.difficulty(),
                List.of(new AddQuestionVersionCommand.KnowledgePointInput(null, request.knowledgePointId(),
                        BigDecimal.ONE))));
    }

    private List<QuestionOptionPayload> options(JsonNode item) {
        List<QuestionOptionPayload> options = new ArrayList<>();
        for (JsonNode option : item.path("options")) {
            options.add(new QuestionOptionPayload(option.path("optionKey").asText(),
                    option.path("content").asText(), option.path("sortOrder").asInt()));
        }
        return options;
    }

    private JsonNode read(String text) {
        try {
            return text == null ? json.createObjectNode() : json.readTree(text);
        } catch (JsonProcessingException invalid) {
            throw new InvalidContentStateException("Stored draft result is unreadable");
        }
    }

    private QuestionDraftRequest readRequest(AiDraft draft) {
        try {
            return json.readValue(draft.getRequestJson(), QuestionDraftRequest.class);
        } catch (JsonProcessingException invalid) {
            throw new InvalidContentStateException("Stored draft request is unreadable");
        }
    }
}

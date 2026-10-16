package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.ieltspath.assessment.domain.entity.ErrorAnalysisItem;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemKnowledgePointRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.ErrorAnalysisItemRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.service.AnswerSpecGrader;
import com.ieltspath.assessment.domain.service.ErrorTagger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grades a just-submitted attempt when every item is objective. Result version 1, its item results and the outbox
 * row are written in the submit transaction. If any item is ungradable, nothing is written and the attempt waits for
 * a human grader exactly as before.
 */
@Service
public class AutoGradeAttemptService {
    private static final String NO_FEEDBACK = "{}";

    private final AttemptItemRepository attemptItems;
    private final AttemptResponseRepository responses;
    private final AssessmentResultRepository results;
    private final ItemResultRepository itemResults;
    private final ErrorAnalysisItemRepository errors;
    private final AttemptItemKnowledgePointRepository knowledgeSnapshot;
    private final AssessmentResultCompleter completer;
    private final ObjectMapper json;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();
    private final ErrorTagger errorTagger = new ErrorTagger();

    public AutoGradeAttemptService(AttemptItemRepository attemptItems, AttemptResponseRepository responses,
                                   AssessmentResultRepository results, ItemResultRepository itemResults,
                                   ErrorAnalysisItemRepository errors,
                                   AttemptItemKnowledgePointRepository knowledgeSnapshot,
                                   AssessmentResultCompleter completer, ObjectMapper json) {
        this.attemptItems = attemptItems;
        this.responses = responses;
        this.results = results;
        this.itemResults = itemResults;
        this.errors = errors;
        this.knowledgeSnapshot = knowledgeSnapshot;
        this.completer = completer;
        this.json = json;
    }

    /** @return true when the attempt was graded and its result completed. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean gradeIfObjective(AssessmentAttempt attempt) {
        return grade(attempt, Map.of());
    }

    /**
     * Grades the objective items and takes each essay's outcome from {@code essayPassed} (keyed by attempt item): a
     * passed essay earns its full score, any other none. Writes nothing and returns false when an item is neither
     * objective nor an essay with an outcome.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean grade(AssessmentAttempt attempt, Map<UUID, Boolean> essayPassed) {
        List<AttemptItem> items = attemptItems.findByAttemptId(attempt.getId());
        if (items.isEmpty()) {
            return false;
        }
        List<AnswerSnapshot> answers = new ArrayList<>(items.size());
        for (AttemptItem item : items) {
            Optional<AnswerSnapshot> answer = AnswerSnapshot.parse(json, item.answerSnapshot());
            boolean essay = answer.isPresent() && essayPassed.containsKey(item.id()) && answer.get().gradableEssay();
            if (answer.isEmpty() || !essay && !answer.get().autoGradable(grader)) {
                return false;
            }
            answers.add(answer.get());
        }

        Map<UUID, AttemptResponse> responseByItem = responses.findByAttemptItemIds(
                        items.stream().map(AttemptItem::id).toList()).stream()
                .collect(Collectors.toMap(AttemptResponse::attemptItemId, Function.identity()));
        AssessmentResult draft = results.save(new AssessmentResult(UUID.randomUUID(), attempt.getId(), 1,
                AssessmentResult.DRAFT, null, null));
        Map<UUID, Long> elapsedByItem = attemptItems.elapsedMsByItemId(items.stream().map(AttemptItem::id).toList());
        List<ItemResult> graded = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            AttemptItem item = items.get(i);
            AnswerSnapshot answer = answers.get(i);
            boolean correct = essayPassed.containsKey(item.id()) ? Boolean.TRUE.equals(essayPassed.get(item.id()))
                    : isCorrect(answer, responseByItem.get(item.id()));
            graded.add(new ItemResult(UUID.randomUUID(), draft.id(), item.id(), correct ? answer.maxScore() : 0.0,
                    answer.maxScore(), correct, elapsedByItem.get(item.id()), NO_FEEDBACK));
        }
        itemResults.saveAll(graded);
        List<ErrorAnalysisItem> tags = errorAnalysis(items, answers, responseByItem, essayPassed, graded);
        if (!tags.isEmpty()) {
            errors.saveAll(tags);
        }
        completer.complete(attempt, draft, items, graded);
        return true;
    }

    /**
     * One error row per knowledge point of each wrong objective item that has an error tag. An item with no mapped
     * knowledge point still gets one row, with a null knowledge point, so the tag is not lost.
     */
    private List<ErrorAnalysisItem> errorAnalysis(List<AttemptItem> items, List<AnswerSnapshot> answers,
                                                  Map<UUID, AttemptResponse> responseByItem,
                                                  Map<UUID, Boolean> essayPassed, List<ItemResult> graded) {
        Map<UUID, List<UUID>> knowledgeByItem = knowledgeSnapshot.findByAttemptItemIds(
                        items.stream().map(AttemptItem::id).toList()).stream()
                .collect(Collectors.groupingBy(AttemptItemKnowledgePoint::attemptItemId,
                        Collectors.mapping(AttemptItemKnowledgePoint::knowledgePointId, Collectors.toList())));
        List<ErrorAnalysisItem> rows = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            AttemptItem item = items.get(i);
            if (essayPassed.containsKey(item.id()) || graded.get(i).correct()) {
                continue;
            }
            String tag = errorTagger.tag(answers.get(i).answerSpec(),
                    responseAnswer(responseByItem.get(item.id())), false);
            if (tag == null) {
                continue;
            }
            List<UUID> knowledgePoints = knowledgeByItem.getOrDefault(item.id(), List.of());
            if (knowledgePoints.isEmpty()) {
                rows.add(new ErrorAnalysisItem(UUID.randomUUID(), graded.get(i).id(), null, tag, null));
            }
            for (UUID knowledgePoint : knowledgePoints) {
                rows.add(new ErrorAnalysisItem(UUID.randomUUID(), graded.get(i).id(), knowledgePoint, tag, null));
            }
        }
        return rows;
    }

    /** The submitted {@code answer} string of a stored response, or null when omitted or not a string. */
    private String responseAnswer(AttemptResponse response) {
        if (response == null) {
            return null;
        }
        try {
            return ErrorTagger.answerOf(json.readValue(response.payload(), Object.class));
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * A missing response, {@code {}} or {@code {"answer":null}} is an omitted answer. A payload whose {@code answer}
     * is not a string is invalid input; it scores zero rather than being coerced into a match.
     */
    boolean isCorrect(AnswerSnapshot answer, AttemptResponse response) {
        Object payload = null;
        if (response != null) {
            try {
                payload = json.readValue(response.payload(), Object.class);
            } catch (Exception exception) {
                return false;
            }
            if (!(payload instanceof Map<?, ?>)) {
                return false;
            }
        }
        try {
            return grader.grade(answer.answerSpec(), payload).correct();
        } catch (IllegalArgumentException invalidAnswer) {
            return false;
        }
    }
}

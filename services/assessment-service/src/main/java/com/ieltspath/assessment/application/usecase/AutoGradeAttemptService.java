package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.service.AnswerSpecGrader;
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
    private final AssessmentResultCompleter completer;
    private final ObjectMapper json;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public AutoGradeAttemptService(AttemptItemRepository attemptItems, AttemptResponseRepository responses,
                                   AssessmentResultRepository results, ItemResultRepository itemResults,
                                   AssessmentResultCompleter completer, ObjectMapper json) {
        this.attemptItems = attemptItems;
        this.responses = responses;
        this.results = results;
        this.itemResults = itemResults;
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
        List<ItemResult> graded = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            AttemptItem item = items.get(i);
            AnswerSnapshot answer = answers.get(i);
            boolean correct = essayPassed.containsKey(item.id()) ? Boolean.TRUE.equals(essayPassed.get(item.id()))
                    : isCorrect(answer, responseByItem.get(item.id()));
            graded.add(new ItemResult(UUID.randomUUID(), draft.id(), item.id(), correct ? answer.maxScore() : 0.0,
                    answer.maxScore(), correct, null, NO_FEEDBACK));
        }
        itemResults.saveAll(graded);
        completer.complete(attempt, draft, items, graded);
        return true;
    }

    /**
     * A missing response, {@code {}} or {@code {"answer":null}} is an omitted answer. A payload whose {@code answer}
     * is not a string is invalid input; it scores zero rather than being coerced into a match.
     */
    private boolean isCorrect(AnswerSnapshot answer, AttemptResponse response) {
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

package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.service.PlacementBandCalculator;
import com.ieltspath.assessment.domain.vo.AttemptType;
import com.ieltspath.assessment.domain.vo.GradingJobStatus;
import com.ieltspath.assessment.domain.vo.Skill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grades a placement attempt and writes its overall band on the result. Listening and Reading are graded by percent
 * correct, Writing by the LLM band of the essay the learner sent (zero when none was sent) and Speaking by a fixed band
 * until recordings can be assessed. The skill bands are averaged into the result's {@code overallBand}; Learning only
 * stores it. An essay the LLM could not grade takes the default Writing band instead of waiting for an examiner.
 */
@Service
public class PlacementGradingService {
    private static final String NO_FEEDBACK = "{}";

    private final AssessmentAttemptRepository attempts;
    private final AttemptSectionRepository sections;
    private final AttemptItemRepository attemptItems;
    private final AttemptResponseRepository responses;
    private final AssessmentResultRepository results;
    private final ItemResultRepository itemResults;
    private final AssessmentResultCompleter completer;
    private final GateEssayJobStore jobs;
    private final AutoGradeAttemptService autoGrader;
    private final ObjectMapper json;
    private final double speakingBand;
    private final BigDecimal defaultWritingBand;

    public PlacementGradingService(AssessmentAttemptRepository attempts, AttemptSectionRepository sections,
                                   AttemptItemRepository attemptItems, AttemptResponseRepository responses,
                                   AssessmentResultRepository results, ItemResultRepository itemResults,
                                   AssessmentResultCompleter completer, GateEssayJobStore jobs,
                                   AutoGradeAttemptService autoGrader, ObjectMapper json,
                                   @Value("${assessment.placement.speaking-band:5.5}") double speakingBand,
                                   @Value("${assessment.placement.default-writing-band:5.5}") double defaultWritingBand) {
        this.attempts = attempts;
        this.sections = sections;
        this.attemptItems = attemptItems;
        this.responses = responses;
        this.results = results;
        this.itemResults = itemResults;
        this.completer = completer;
        this.jobs = jobs;
        this.autoGrader = autoGrader;
        this.json = json;
        this.speakingBand = speakingBand;
        this.defaultWritingBand = BigDecimal.valueOf(defaultWritingBand);
    }

    public BigDecimal defaultWritingBand() {
        return defaultWritingBand;
    }

    public boolean handles(UUID attemptId) {
        return attempts.findById(attemptId).map(a -> a.getAttemptType() == AttemptType.PLACEMENT).orElse(false);
    }

    /** Queues the sent essays for the LLM, or grades at once when the learner sent none. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void onSubmit(AssessmentAttempt attempt) {
        List<AttemptItem> items = attemptItems.findByAttemptId(attempt.getId());
        List<UUID> essayItems = items.stream()
                .filter(item -> AnswerSnapshot.parse(json, item.answerSnapshot())
                        .filter(AnswerSnapshot::gradableEssay).isPresent())
                .map(AttemptItem::id).toList();
        Map<UUID, UUID> submitted = jobs.submittedEssays(essayItems);
        if (submitted.isEmpty()) {
            finish(attempt, items, Map.of());
            return;
        }
        jobs.enqueueAi(submitted, attempt.getUserId());
    }

    /** @return true when this call completed the result. */
    @Transactional
    public boolean completeIfGraded(UUID attemptId) {
        var attempt = attempts.findById(attemptId).orElse(null);
        if (attempt == null || results.findLatestForUpdateByAttemptId(attemptId).isPresent()) return false;
        List<GateEssayJobStore.JobState> states = jobs.aiJobs(attemptId);
        if (states.isEmpty() || states.stream()
                .anyMatch(state -> !GradingJobStatus.COMPLETED.name().equals(state.status()))) {
            return false;
        }
        Map<UUID, BigDecimal> bands = new HashMap<>();
        states.forEach(state -> bands.put(state.attemptItemId(),
                state.band() != null ? state.band() : defaultWritingBand));
        finish(attempt, attemptItems.findByAttemptId(attemptId), bands);
        return true;
    }

    private void finish(AssessmentAttempt attempt, List<AttemptItem> items, Map<UUID, BigDecimal> essayBands) {
        Map<UUID, Skill> skillBySection = sections.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(AttemptSection::id, this::skillOf));
        Map<UUID, AttemptResponse> responseByItem = responses.findByAttemptItemIds(
                        items.stream().map(AttemptItem::id).toList()).stream()
                .collect(Collectors.toMap(AttemptResponse::attemptItemId, Function.identity()));

        UUID resultId = UUID.randomUUID();
        Map<Skill, int[]> objective = new EnumMap<>(Skill.class);
        Map<Skill, List<Double>> banded = new EnumMap<>(Skill.class);
        List<ItemResult> graded = new ArrayList<>(items.size());
        for (AttemptItem item : items) {
            AnswerSnapshot answer = AnswerSnapshot.parse(json, item.answerSnapshot())
                    .filter(snapshot -> snapshot.maxScore() != null && snapshot.maxScore() > 0)
                    .orElseThrow(() -> new InvalidAssessmentStateException("Placement item has no answer snapshot"));
            Skill skill = skillBySection.get(item.attemptSectionId());
            boolean correct;
            if (skill == Skill.WRITING) {
                double band = essayBands.getOrDefault(item.id(), BigDecimal.ZERO).doubleValue();
                banded.computeIfAbsent(skill, key -> new ArrayList<>()).add(band);
                correct = answer.passBand() != null && band >= answer.passBand().doubleValue();
            } else if (skill == Skill.SPEAKING) {
                banded.computeIfAbsent(skill, key -> new ArrayList<>()).add(speakingBand);
                correct = true;
            } else {
                correct = autoGrader.isCorrect(answer, responseByItem.get(item.id()));
                int[] counts = objective.computeIfAbsent(skill, key -> new int[2]);
                counts[0] += correct ? 1 : 0;
                counts[1]++;
            }
            graded.add(new ItemResult(UUID.randomUUID(), resultId, item.id(), correct ? answer.maxScore() : 0.0,
                    answer.maxScore(), correct, null, NO_FEEDBACK));
        }
        AssessmentResult draft = results.save(new AssessmentResult(resultId, attempt.getId(), 1,
                AssessmentResult.DRAFT, PlacementBandCalculator.overallBand(bandsBySkill(objective, banded).values()),
                null));
        itemResults.saveAll(graded);
        completer.complete(attempt, draft, items, graded);
    }

    /**
     * The skill bands behind a completed placement result, recomputed the way {@link #finish} computed them: objective
     * skills from the stored item correctness, Writing from the essay jobs (zero when no essay was sent) and Speaking
     * from the fixed band.
     */
    @Transactional(readOnly = true)
    public Map<Skill, Double> skillBands(UUID attemptId, UUID resultId) {
        Map<UUID, Skill> skillBySection = sections.findByAttemptId(attemptId).stream()
                .collect(Collectors.toMap(AttemptSection::id, this::skillOf));
        Map<UUID, ItemResult> gradedByItem = itemResults.findByResultId(resultId).stream()
                .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
        Map<UUID, BigDecimal> essayBands = new HashMap<>();
        jobs.aiJobs(attemptId).forEach(state -> essayBands.put(state.attemptItemId(),
                state.band() != null ? state.band() : defaultWritingBand));

        Map<Skill, int[]> objective = new EnumMap<>(Skill.class);
        Map<Skill, List<Double>> banded = new EnumMap<>(Skill.class);
        for (AttemptItem item : attemptItems.findByAttemptId(attemptId)) {
            Skill skill = skillBySection.get(item.attemptSectionId());
            if (skill == Skill.WRITING) {
                banded.computeIfAbsent(skill, key -> new ArrayList<>())
                        .add(essayBands.getOrDefault(item.id(), BigDecimal.ZERO).doubleValue());
            } else if (skill == Skill.SPEAKING) {
                banded.computeIfAbsent(skill, key -> new ArrayList<>()).add(speakingBand);
            } else {
                ItemResult graded = gradedByItem.get(item.id());
                int[] counts = objective.computeIfAbsent(skill, key -> new int[2]);
                counts[0] += graded != null && Boolean.TRUE.equals(graded.correct()) ? 1 : 0;
                counts[1]++;
            }
        }
        return bandsBySkill(objective, banded);
    }

    private static Map<Skill, Double> bandsBySkill(Map<Skill, int[]> objective, Map<Skill, List<Double>> banded) {
        Map<Skill, Double> bands = new EnumMap<>(Skill.class);
        objective.forEach((skill, c) -> bands.put(skill, PlacementBandCalculator.bandForPercent(100.0 * c[0] / c[1])));
        banded.forEach((skill, list) -> bands.put(skill, list.stream().mapToDouble(Double::doubleValue)
                .average().orElse(0.0)));
        return bands;
    }

    private Skill skillOf(AttemptSection section) {
        try {
            JsonNode node = json.readTree(section.snapshot());
            return Skill.valueOf(node.path("skill").asText());
        } catch (Exception exception) {
            throw new InvalidAssessmentStateException("Placement section has no valid skill");
        }
    }
}

package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
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
import com.ieltspath.assessment.domain.repository.SkillScoreRepository;
import com.ieltspath.assessment.domain.entity.SkillScore;
import com.ieltspath.assessment.domain.vo.GradingSource;
import com.ieltspath.assessment.domain.vo.PlacementPool;
import com.ieltspath.assessment.domain.service.PlacementBandCeiling;
import com.ieltspath.assessment.domain.service.PlacementBandCalculator;
import com.ieltspath.assessment.domain.vo.AttemptType;
import com.ieltspath.assessment.domain.vo.GradingJobStatus;
import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grades the skills that were actually assessed, persists those bands, and caps the overall band at the selected pool.
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
    private final SkillScoreRepository skillScores;
    private final AssessmentResultCompleter completer;
    private final GateEssayJobStore jobs;
    private final AutoGradeAttemptService autoGrader;
    private final ObjectMapper json;
    private final Double speakingBand;
    private final BigDecimal defaultWritingBand;
    private final PlacementSpeakingGradeStore speakingGrades;
    private final boolean speakingGradingEnabled;
    private final PlacementBandCeiling ceiling = new PlacementBandCeiling();

    @Autowired
    public PlacementGradingService(AssessmentAttemptRepository attempts, AttemptSectionRepository sections,
                                   AttemptItemRepository attemptItems, AttemptResponseRepository responses,
                                   AssessmentResultRepository results, ItemResultRepository itemResults,
                                   AssessmentResultCompleter completer, GateEssayJobStore jobs,
                                   AutoGradeAttemptService autoGrader, ObjectMapper json, SkillScoreRepository skillScores,
                                   @Value("${assessment.placement.speaking-band:#{null}}") Double speakingBand,
                                   @Value("${assessment.placement.default-writing-band:5.5}") double defaultWritingBand,
                                   PlacementSpeakingGradeStore speakingGrades,
                                   @Value("${assessment.placement.speaking-grading.enabled:false}") boolean speakingGradingEnabled) {
        this.attempts = attempts;
        this.sections = sections;
        this.attemptItems = attemptItems;
        this.responses = responses;
        this.results = results;
        this.itemResults = itemResults;
        this.skillScores = skillScores;
        this.completer = completer;
        this.jobs = jobs;
        this.autoGrader = autoGrader;
        this.json = json;
        this.speakingBand = speakingBand;
        this.defaultWritingBand = BigDecimal.valueOf(defaultWritingBand);
        this.speakingGrades = speakingGrades;
        this.speakingGradingEnabled = speakingGradingEnabled;
    }

    /** Compatibility constructor for tests and callers that exercise the legacy disabled path. */
    public PlacementGradingService(AssessmentAttemptRepository attempts, AttemptSectionRepository sections,
                                   AttemptItemRepository attemptItems, AttemptResponseRepository responses,
                                   AssessmentResultRepository results, ItemResultRepository itemResults,
                                   AssessmentResultCompleter completer, GateEssayJobStore jobs,
                                   AutoGradeAttemptService autoGrader, ObjectMapper json, SkillScoreRepository skillScores,
                                   Double speakingBand, double defaultWritingBand) {
        this(attempts, sections, attemptItems, responses, results, itemResults, completer, jobs, autoGrader, json,
                skillScores, speakingBand, defaultWritingBand, null, false);
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
        boolean speakingRequested = speakingGradingEnabled && speakingGrades != null
                && !speakingGrades.recordings(attempt.getId(), attempt.getUserId()).isEmpty();
        if (speakingRequested) {
            var recordings = speakingGrades.recordings(attempt.getId(), attempt.getUserId()).stream()
                    .map(item -> new SpeakingGradingRequestedV1.Item(item.attemptItemId(), item.partNo(),
                            item.questionNo(), item.question(), item.objectKey())).toList();
            var event = new SpeakingGradingRequestedV1(UUID.randomUUID(), SpeakingGradingRequestedV1.EVENT_TYPE,
                    Instant.now(), SpeakingGradingRequestedV1.SOURCE,
                    new SpeakingGradingRequestedV1.Data(attempt.getId(), attempt.getUserId(), recordings));
            speakingGrades.enqueue(event, Instant.now().plusSeconds(600));
        }
        if (submitted.isEmpty() && !speakingRequested) {
            finish(attempt, items, Map.of(), Set.of(), null);
            return;
        }
        if (!submitted.isEmpty()) jobs.enqueueAi(submitted, attempt.getUserId());
    }

    /** @return true when this call completed the result. */
    @Transactional
    public boolean completeIfGraded(UUID attemptId) {
        var attempt = attempts.findById(attemptId).orElse(null);
        if (attempt == null || results.findLatestForUpdateByAttemptId(attemptId).isPresent()) return false;
        PlacementSpeakingGradeStore.Grade speakingGrade = speakingGrades == null ? null
                : speakingGrades.find(attemptId).orElse(null);
        if (speakingGrade != null && "PENDING".equals(speakingGrade.status())) return false;
        List<GateEssayJobStore.JobState> states = jobs.aiJobs(attemptId);
        if (!states.isEmpty() && states.stream()
                .anyMatch(state -> !GradingJobStatus.COMPLETED.name().equals(state.status()))) {
            return false;
        }
        Map<UUID, BigDecimal> bands = new HashMap<>();
        states.stream().filter(state -> state.band() != null)
                .forEach(state -> bands.put(state.attemptItemId(), state.band()));
        Set<UUID> sentEssays = states.stream().map(GateEssayJobStore.JobState::attemptItemId)
                .collect(Collectors.toSet());
        finish(attempt, attemptItems.findByAttemptId(attemptId), bands, sentEssays, speakingGrade);
        return true;
    }

    /**
     * @param essayBands  LLM band of each essay that was graded
     * @param sentEssays  every essay the learner sent; one of these without a band could not be graded
     */
    private void finish(AssessmentAttempt attempt, List<AttemptItem> items, Map<UUID, BigDecimal> essayBands,
                        Set<UUID> sentEssays, PlacementSpeakingGradeStore.Grade speakingGrade) {
        Map<UUID, Skill> skillBySection = sections.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(AttemptSection::id, this::skillOf));
        Map<UUID, AttemptResponse> responseByItem = responses.findByAttemptItemIds(
                        items.stream().map(AttemptItem::id).toList()).stream()
                .collect(Collectors.toMap(AttemptResponse::attemptItemId, Function.identity()));

        UUID resultId = UUID.randomUUID();
        Map<Skill, int[]> objective = new EnumMap<>(Skill.class);
        Map<Skill, double[]> banded = new EnumMap<>(Skill.class);
        boolean writingUngraded = false;
        boolean speakingUngraded = false;
        List<ItemResult> graded = new ArrayList<>(items.size());
        for (AttemptItem item : items) {
            AnswerSnapshot answer = AnswerSnapshot.parse(json, item.answerSnapshot())
                    .filter(snapshot -> snapshot.maxScore() != null && snapshot.maxScore() > 0)
                    .orElseThrow(() -> new InvalidAssessmentStateException("Câu hỏi bài kiểm tra đầu vào thiếu dữ liệu đáp án."));
            Skill skill = skillBySection.get(item.attemptSectionId());
            boolean correct;
            Boolean judged;
            if (skill == Skill.WRITING) {
                BigDecimal essayBand = essayBands.get(item.id());
                if (essayBand == null) {
                    boolean sent = sentEssays.contains(item.id());
                    if (sent) writingUngraded = true;
                    else addBand(banded, skill, 0.0, writingWeight(answer));
                    correct = false;
                    judged = sent ? null : false;
                } else {
                    double band = essayBand.doubleValue();
                    addBand(banded, skill, band, writingWeight(answer));
                    correct = answer.passBand() != null && band >= answer.passBand().doubleValue();
                    judged = correct;
                }
            } else if (skill == Skill.SPEAKING) {
                Double actualBand = speakingGrade != null && "GRADED".equals(speakingGrade.status())
                        ? speakingGrade.band() : speakingGradingEnabled ? null : speakingBand;
                if (actualBand == null) {
                    correct = false;
                    judged = null;
                    if (speakingGrade != null && "FAILED".equals(speakingGrade.status())) speakingUngraded = true;
                } else {
                    addBand(banded, skill, actualBand, 1.0);
                    correct = true;
                    judged = true;
                }
            } else {
                correct = autoGrader.isCorrect(answer, responseByItem.get(item.id()));
                judged = correct;
                int[] counts = objective.computeIfAbsent(skill, key -> new int[2]);
                counts[0] += correct ? 1 : 0;
                counts[1]++;
            }
            graded.add(new ItemResult(UUID.randomUUID(), resultId, item.id(), correct ? answer.maxScore() : 0.0,
                    answer.maxScore(), judged, null, NO_FEEDBACK));
        }
        // One essay the LLM could not grade leaves Writing unknown: the graded part alone must not stand in for it.
        boolean partial = writingUngraded || speakingUngraded;
        if (writingUngraded) banded.remove(Skill.WRITING);
        Map<Skill, Double> skillBands = bandsBySkill(objective, banded);
        double uncapped = PlacementBandCalculator.overallBand(skillBands.values());
        PlacementPool pool = attempt.getPlacementPool();
        PlacementBandCeiling.CeilingResult capped = ceiling.apply(uncapped, pool);
        AssessmentResult draft = results.save(new AssessmentResult(resultId, attempt.getId(), 1,
                AssessmentResult.DRAFT, capped.band(), null, pool, uncapped, partial));
        skillScores.saveAll(skillBands.entrySet().stream().map(entry -> new SkillScore(UUID.randomUUID(), resultId,
                entry.getKey(), null, entry.getValue(), sourceOf(entry.getKey(), essayBands, speakingGrade),
                null)).toList());
        itemResults.saveAll(graded);
        completer.complete(attempt, draft, items, graded);
    }

    /**
     * Returns the bands saved at grading time. Old results without score rows retain the former read behavior.
     */
    @Transactional(readOnly = true)
    public Map<Skill, Double> skillBands(UUID attemptId, UUID resultId) {
        List<SkillScore> stored = skillScores.findByResultId(resultId);
        if (!stored.isEmpty()) {
            Map<Skill, Double> saved = new EnumMap<>(Skill.class);
            stored.forEach(score -> saved.put(score.skill(), score.band()));
            return saved;
        }
        Map<UUID, Skill> skillBySection = sections.findByAttemptId(attemptId).stream()
                .collect(Collectors.toMap(AttemptSection::id, this::skillOf));
        Map<UUID, ItemResult> gradedByItem = itemResults.findByResultId(resultId).stream()
                .collect(Collectors.toMap(ItemResult::attemptItemId, Function.identity()));
        Map<UUID, BigDecimal> essayBands = new HashMap<>();
        jobs.aiJobs(attemptId).forEach(state -> essayBands.put(state.attemptItemId(),
                state.band() != null ? state.band() : defaultWritingBand));

        Map<Skill, int[]> objective = new EnumMap<>(Skill.class);
        Map<Skill, double[]> banded = new EnumMap<>(Skill.class);
        for (AttemptItem item : attemptItems.findByAttemptId(attemptId)) {
            Skill skill = skillBySection.get(item.attemptSectionId());
            if (skill == Skill.WRITING) {
                double weight = AnswerSnapshot.parse(json, item.answerSnapshot()).map(this::writingWeight).orElse(1.0);
                addBand(banded, skill, essayBands.getOrDefault(item.id(), BigDecimal.ZERO).doubleValue(), weight);
            } else if (skill == Skill.SPEAKING) {
                addBand(banded, skill, speakingBand == null ? 0.0 : speakingBand, 1.0);
            } else {
                ItemResult graded = gradedByItem.get(item.id());
                int[] counts = objective.computeIfAbsent(skill, key -> new int[2]);
                counts[0] += graded != null && Boolean.TRUE.equals(graded.correct()) ? 1 : 0;
                counts[1]++;
            }
        }
        return bandsBySkill(objective, banded);
    }

    /**
     * AI only for a Writing band that comes from an essay the LLM graded. Every other band is computed: Listening and
     * Reading from the correct answers, Writing from essays that were not sent (zero), Speaking from its configured band.
     */
    private static GradingSource sourceOf(Skill skill, Map<UUID, BigDecimal> essayBands,
                                          PlacementSpeakingGradeStore.Grade speakingGrade) {
        return skill == Skill.SPEAKING && speakingGrade != null && "GRADED".equals(speakingGrade.status())
                || skill == Skill.WRITING && !essayBands.isEmpty() ? GradingSource.AI : GradingSource.AUTO;
    }

    private double writingWeight(AnswerSnapshot answer) {
        return PlacementBandCalculator.writingTaskWeight(answer.answerSpec() == null ? null : answer.answerSpec().get("task"));
    }

    /** Adds a weighted band to {@code [sum of band x weight, sum of weights]} of the skill. */
    private static void addBand(Map<Skill, double[]> banded, Skill skill, double band, double weight) {
        double[] totals = banded.computeIfAbsent(skill, key -> new double[2]);
        totals[0] += band * weight;
        totals[1] += weight;
    }

    private static Map<Skill, Double> bandsBySkill(Map<Skill, int[]> objective, Map<Skill, double[]> banded) {
        Map<Skill, Double> bands = new EnumMap<>(Skill.class);
        objective.forEach((skill, c) -> bands.put(skill, PlacementBandCalculator.bandForCorrect(skill, c[0], c[1])));
        banded.forEach((skill, totals) -> bands.put(skill, totals[1] > 0 ? totals[0] / totals[1] : 0.0));
        return bands;
    }

    private Skill skillOf(AttemptSection section) {
        try {
            JsonNode node = json.readTree(section.snapshot());
            return Skill.valueOf(node.path("skill").asText());
        } catch (Exception exception) {
            throw new InvalidAssessmentStateException("Phần thi kiểm tra đầu vào chưa có kỹ năng hợp lệ.");
        }
    }
}

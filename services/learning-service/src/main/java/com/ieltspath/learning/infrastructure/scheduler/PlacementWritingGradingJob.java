package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.assessment.PlacementWritingGradingCompletedV1;
import com.ieltspath.events.assessment.PlacementWritingGradingRequestedV1;
import com.ieltspath.learning.application.exception.WritingGradingException;
import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import com.ieltspath.learning.application.port.PlacementWritingJobStore;
import com.ieltspath.learning.application.result.PlacementEssayGrade;
import com.ieltspath.learning.application.service.EssayGrader;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Grades placement essays with the grader of course and free practice, calling the LLM only after the claim
 * transaction ends. Nothing is charged and no learning evidence is recorded: the placement result carries that.
 */
@Component
@EnableScheduling
public class PlacementWritingGradingJob {
    private static final Logger log = LoggerFactory.getLogger(PlacementWritingGradingJob.class);
    private final PlacementWritingJobStore jobs;
    private final EssayGrader grader;
    private final ObjectMapper json;
    private final int maxAttempts;
    private final int batchSize;
    private final int retrySeconds;
    private final int staleSeconds;
    private final int giveUpSeconds;

    public PlacementWritingGradingJob(PlacementWritingJobStore jobs, EssayGrader grader, ObjectMapper json,
                                      @Value("${learning.placement-writing.max-attempts:2}") int maxAttempts,
                                      @Value("${learning.placement-writing.batch-size:5}") int batchSize,
                                      @Value("${learning.placement-writing.retry-delay-seconds:30}") int retrySeconds,
                                      @Value("${learning.placement-writing.stale-after-seconds:300}") int staleSeconds,
                                      @Value("${learning.placement-writing.give-up-after-seconds:540}") int giveUpSeconds) {
        this.jobs = jobs;
        this.grader = grader;
        this.json = json;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
        this.retrySeconds = retrySeconds;
        this.staleSeconds = staleSeconds;
        this.giveUpSeconds = giveUpSeconds;
    }

    @Scheduled(fixedDelayString = "${learning.placement-writing.poll-ms:5000}")
    public void poll() {
        Instant now = Instant.now();
        jobs.claimDue(batchSize, maxAttempts, now, now.minusSeconds(staleSeconds)).forEach(this::process);
    }

    private void process(PlacementSpeakingJobStore.Job job) {
        try {
            PlacementWritingGradingRequestedV1 request =
                    json.readValue(job.payload(), PlacementWritingGradingRequestedV1.class);
            List<PlacementEssayGrade> essays = new ArrayList<>();
            Map<UUID, String> versions = new java.util.HashMap<>();
            // Assessment grades an essay itself once it has waited 10 minutes; grading it here as well would cost a
            // model call for a grade that is never used.
            boolean late = request.occurredAt() != null
                    && request.occurredAt().isBefore(Instant.now().minusSeconds(giveUpSeconds));
            for (var item : request.data().items()) {
                essays.add(late ? new PlacementEssayGrade(item.attemptItemId(), item.task(), 0, null, item.essayText(),
                        null, "TOO_LATE") : grade(item, versions));
            }
            String failure = essays.stream().map(PlacementEssayGrade::failureCode)
                    .filter(java.util.Objects::nonNull).findFirst().orElse(null);
            // Only a run where nothing was graded is tried again: a graded essay is never paid for twice, and an
            // essay that failed alone goes back to Assessment's own grader.
            boolean noneGraded = essays.stream().allMatch(essay -> essay.grade() == null);
            if (failure != null && noneGraded && !late && job.attempts() < maxAttempts) {
                jobs.retry(job.eventId(), failure, Instant.now().plusSeconds(retrySeconds));
                log.warn("Placement Writing job deferred: eventId={}, attempt={}, code={}", job.eventId(),
                        job.attempts(), failure);
                return;
            }
            publish(job, essays, versions, failure);
        } catch (Exception unexpected) {
            // The claim stays PROCESSING until it is stale; Assessment grades locally if nothing ever arrives.
            log.error("Placement Writing job failed: eventId={}, errorType={}", job.eventId(),
                    unexpected.getClass().getSimpleName());
            jobs.retry(job.eventId(), "GRADING_FAILED", Instant.now().plusSeconds(retrySeconds));
        }
    }

    private PlacementEssayGrade grade(PlacementWritingGradingRequestedV1.Item item, Map<UUID, String> versions) {
        String essay = item.essayText();
        int words = essay.strip().split("\\s+").length;
        try {
            if (!grader.available()) throw new WritingGradingException("NOT_CONFIGURED");
            var graded = grader.grade(new EssayPrompt(item.questionVersionId(), item.stem(), item.task(),
                    item.minWords(), item.passBand(), item.chartFacts(), null, List.of(), item.knowledgePointIds()),
                    essay);
            WritingGrade grade = graded.grade();
            versions.put(item.attemptItemId(), graded.promptVersion());
            Boolean passed = item.passBand() == null || grade.overallBand() == null ? null
                    : grade.overallBand().compareTo(item.passBand()) >= 0;
            return new PlacementEssayGrade(item.attemptItemId(), item.task(), words, passed, essay, grade, null);
        } catch (RuntimeException failure) {
            String code = failure instanceof WritingGradingException grading ? grading.getCode() : "GRADING_FAILED";
            return new PlacementEssayGrade(item.attemptItemId(), item.task(), words, null, essay, null, code);
        }
    }

    private void publish(PlacementSpeakingJobStore.Job job, List<PlacementEssayGrade> essays,
                         Map<UUID, String> versions, String failure) throws Exception {
        Instant now = Instant.now();
        var items = essays.stream().map(essay -> summary(essay, versions.get(essay.attemptItemId()))).toList();
        var event = new PlacementWritingGradingCompletedV1(UUID.randomUUID(),
                PlacementWritingGradingCompletedV1.EVENT_TYPE, now, PlacementWritingGradingCompletedV1.SOURCE,
                new PlacementWritingGradingCompletedV1.Data(job.attemptId(), job.userId(), items));
        boolean anyGraded = essays.stream().anyMatch(essay -> essay.grade() != null);
        jobs.complete(job.eventId(), anyGraded ? "GRADED" : "FAILED", failure, json.writeValueAsString(essays),
                new PlacementSpeakingJobStore.OutboxEvent(event.eventId(), event.eventType(),
                        json.writeValueAsString(event), now, job.correlationId(), job.traceContext()));
    }

    /** What Assessment keeps: the band and a short comment per criterion. */
    private static PlacementWritingGradingCompletedV1.Item summary(PlacementEssayGrade essay, String promptVersion) {
        WritingGrade grade = essay.grade();
        if (grade == null || grade.overallBand() == null) {
            return new PlacementWritingGradingCompletedV1.Item(essay.attemptItemId(),
                    PlacementWritingGradingCompletedV1.FAILED, null, null, List.of(), List.of(), null,
                    essay.failureCode() == null ? "GRADING_FAILED" : essay.failureCode());
        }
        var criteria = grade.criteria().stream().map(criterion -> new PlacementWritingGradingCompletedV1.Criterion(
                criterion.code(), criterion.band(), java.util.stream.Stream
                .concat(criterion.strengths().stream(), criterion.improvements().stream())
                .collect(Collectors.joining(" ")))).toList();
        var focus = grade.priorities() == null ? List.<String>of()
                : grade.priorities().stream().map(WritingGrade.Priority::title).toList();
        return new PlacementWritingGradingCompletedV1.Item(essay.attemptItemId(),
                PlacementWritingGradingCompletedV1.GRADED, grade.overallBand(), grade.summary(), criteria, focus,
                promptVersion, null);
    }
}

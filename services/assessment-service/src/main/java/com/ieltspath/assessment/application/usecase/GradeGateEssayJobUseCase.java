package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.exception.EssayGradingException;
import com.ieltspath.assessment.application.port.AssessmentLlmQuota;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.PromptActivationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Grades queued essay jobs of topic, course and placement tests. Claiming and recording run in short transactions; the
 * quota and the LLM call run outside any transaction. A job that cannot be graded by the LLM (not configured, daily
 * limit, failure or unusable reply) fails and is replaced by a job for a human examiner; a placement essay instead
 * takes the default Writing band so the learner is never left waiting.
 */
@Service
public class GradeGateEssayJobUseCase {
    private static final Logger log = LoggerFactory.getLogger(GradeGateEssayJobUseCase.class);

    private final GateEssayJobStore jobs;
    private final PromptActivationProvider activations;
    private final EssayGradingPort grader;
    private final AssessmentLlmQuota quota;
    private final GateResultAssembler assembler;
    private final PlacementGradingService placement;
    private final TransactionTemplate transaction;
    private final ObjectMapper json;
    private final int batchSize;
    private final int dailyLimit;
    private final Duration stuckAfter;
    private final ZoneId quotaZone;
    private final Clock clock;

    @Autowired
    public GradeGateEssayJobUseCase(GateEssayJobStore jobs, EssayGradingPort grader, AssessmentLlmQuota quota,
                                    GateResultAssembler assembler, PlacementGradingService placement,
                                    PromptActivationProvider activations, PlatformTransactionManager transactionManager, ObjectMapper json,
                                    @Value("${assessment.llm-grading.batch-size:10}") int batchSize,
                                    @Value("${assessment.llm-grading.daily-limit:20}") int dailyLimit,
                                    @Value("${assessment.llm-grading.stuck-after:PT10M}") Duration stuckAfter,
                                    @Value("${assessment.llm-grading.quota-timezone:Asia/Ho_Chi_Minh}") String quotaZone) {
        this(jobs, grader, quota, assembler, placement, activations, transactionManager, json, batchSize, dailyLimit, stuckAfter,
                ZoneId.of(quotaZone), Clock.systemUTC());
    }

    GradeGateEssayJobUseCase(GateEssayJobStore jobs, EssayGradingPort grader, AssessmentLlmQuota quota,
                             GateResultAssembler assembler, PlacementGradingService placement,
                             PromptActivationProvider activations, PlatformTransactionManager transactionManager,
                             ObjectMapper json, int batchSize, int dailyLimit, Duration stuckAfter, ZoneId quotaZone,
                             Clock clock) {
        this.jobs = jobs;
        this.activations = activations;
        this.grader = grader;
        this.quota = quota;
        this.assembler = assembler;
        this.placement = placement;
        this.transaction = new TransactionTemplate(transactionManager);
        this.json = json;
        this.batchSize = batchSize;
        this.dailyLimit = dailyLimit;
        this.stuckAfter = stuckAfter;
        this.quotaZone = quotaZone;
        this.clock = clock;
    }

    GradeGateEssayJobUseCase(GateEssayJobStore jobs, EssayGradingPort grader, AssessmentLlmQuota quota,
                             GateResultAssembler assembler, PlacementGradingService placement,
                             PlatformTransactionManager transactionManager, ObjectMapper json, int batchSize,
                             int dailyLimit, Duration stuckAfter, ZoneId quotaZone, Clock clock) {
        this(jobs, grader, quota, assembler, placement, task -> EssayGradingPort.PROMPT_VERSION,
                transactionManager, json, batchSize, dailyLimit, stuckAfter, quotaZone, clock);
    }

    /** @return how many jobs were claimed. */
    public int gradeBatch() {
        Instant now = clock.instant();
        List<GateEssayJobStore.ClaimedJob> claimed = transaction.execute(status -> {
            jobs.requeueStuck(now.minus(stuckAfter));
            return jobs.claim(batchSize, now);
        });
        if (claimed == null) return 0;
        claimed.forEach(this::gradeOne);
        return claimed.size();
    }

    private void gradeOne(GateEssayJobStore.ClaimedJob job) {
        EssayGradingPort.EssayGrade grade;
        try {
            EssayGradingPort.Prompt gradingPrompt = prompt(job, activatedVersion(job));
            if (!grader.available()) throw new EssayGradingException("LLM_UNAVAILABLE");
            if (!quota.tryConsume(job.userId(), LocalDate.now(clock.withZone(quotaZone)), dailyLimit)) {
                throw new EssayGradingException("DAILY_LIMIT_REACHED");
            }
            grade = grader.grade(gradingPrompt, job.essay());
        } catch (EssayGradingException exception) {
            handOver(job, exception.getCode());
            return;
        } catch (RuntimeException exception) {
            handOver(job, exception.getClass().getSimpleName());
            return;
        }
        transaction.executeWithoutResult(status -> {
            jobs.complete(job.jobId(), grade.band(), grade.feedback(), grade.promptVersion(), clock.instant());
            jobs.recordComparison(job.jobId());
            completeAttempt(job);
        });
    }

    private void completeAttempt(GateEssayJobStore.ClaimedJob job) {
        if (placement.handles(job.attemptId())) {
            placement.completeIfGraded(job.attemptId());
        } else {
            assembler.completeIfGraded(job.attemptId());
        }
    }

    private void handOver(GateEssayJobStore.ClaimedJob job, String code) {
        boolean rubricFailure = switch (code) {
            case "PROMPT_NOT_ACTIVATED", "RUBRIC_UNAVAILABLE", "PROMPT_VERSION_UNAVAILABLE",
                 "ContentUnavailableException", "DataAccessException", "EmptyResultDataAccessException" -> true;
            default -> false;
        };
        if (placement.handles(job.attemptId()) && !rubricFailure) {
            log.warn("Placement essay graded with the default band: jobId={}, code={}", job.jobId(), code);
            transaction.executeWithoutResult(status -> {
                jobs.complete(job.jobId(), placement.defaultWritingBand(), null, clock.instant());
                placement.completeIfGraded(job.attemptId());
            });
            return;
        }
        log.warn("Gate essay grading moved to an examiner: jobId={}, code={}", job.jobId(), code);
        transaction.executeWithoutResult(status -> {
            jobs.fail(job.jobId(), clock.instant());
            String reason = switch (code) {
                case "LLM_UNAVAILABLE" -> "LLM_NOT_CONFIGURED";
                case "DAILY_LIMIT_REACHED" -> "LLM_QUOTA";
                default -> "LLM_FAILED";
            };
            jobs.enqueueHuman(job.submissionId(), job.userId(),
                    EnqueueGateEssayGradingService.humanKey(job.attemptItemId()), reason);
        });
    }

    private String activatedVersion(GateEssayJobStore.ClaimedJob job) {
        try {
            JsonNode spec = json.readTree(job.answerSnapshot()).path("answerSpec");
            String task = spec.path("task").asText(null);
            String version;
            try { version = activations.activatedVersion(task); }
            catch (RuntimeException exception) { throw new EssayGradingException("PROMPT_VERSION_UNAVAILABLE"); }
            // A task bootstraps from Content's ACTIVE rubric until its first approved activation.
            if (version != null && version.isBlank()) throw new EssayGradingException("PROMPT_VERSION_UNAVAILABLE");
            return version;
        } catch (EssayGradingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EssayGradingException("INVALID_PROMPT");
        }
    }

    private EssayGradingPort.Prompt prompt(GateEssayJobStore.ClaimedJob job, String expectedPromptVersion) {
        try {
            JsonNode question = json.readTree(job.questionSnapshot());
            JsonNode spec = json.readTree(job.answerSnapshot()).path("answerSpec");
            return new EssayGradingPort.Prompt(question.path("stem").asText(null), spec.path("task").asText(null),
                    spec.path("minWords").isNumber() ? spec.get("minWords").asInt() : null,
                    spec.path("chartFacts").asText(null), expectedPromptVersion);
        } catch (Exception exception) {
            throw new EssayGradingException("INVALID_PROMPT");
        }
    }
}

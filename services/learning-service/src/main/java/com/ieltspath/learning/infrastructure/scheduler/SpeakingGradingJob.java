package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.exception.SpeechUnavailableException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LlmUsageQuota;
import com.ieltspath.learning.application.port.SpeakingGradingTrigger;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingGrader;
import com.ieltspath.learning.application.service.SpeakingGradingContext;
import com.ieltspath.learning.application.service.SpeakingGradingContextCapture;
import com.ieltspath.learning.application.service.SpeakingGradingInput;
import com.ieltspath.learning.application.service.SpeakingHistorySummarizer;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingPromptAssembler;
import com.ieltspath.learning.application.service.SpeakingResultAssembler;
import com.ieltspath.learning.application.service.SpeakingResultValidator;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.service.PronunciationBandMapper;
import com.ieltspath.learning.domain.service.SpeakingAnalysisSummary;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import com.ieltspath.learning.domain.vo.TranscriptQuality;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Claims and grades Speaking submissions, up to {@code gradingWorkers} at once. A worker is started by a wake-up when a
 * submission is completed and by the periodic poll as a safety net, and a worker that finishes looks for the next
 * submission at once. The claim is {@code FOR UPDATE SKIP LOCKED}, so several workers (or instances) never take the same
 * submission. The recordings of one submission are analysed at the same time ({@code analysisParallelism}); the Azure
 * client's own cap bounds all analyses together. Audio and LLM calls run outside database transactions.
 */
@Component
@EnableScheduling
public class SpeakingGradingJob implements SpeakingGradingTrigger {
    private static final Logger log = LoggerFactory.getLogger(SpeakingGradingJob.class);
    private static final String USAGE_KIND = "speaking_grading";
    /** How long a stopping instance waits for the submissions it is grading; under Spring's shutdown phase and the 40 s grace of the container. */
    static final long SHUTDOWN_WAIT_SECONDS = 25;
    /**
     * A worker and an analysis mostly wait for the network, so a virtual thread each costs almost nothing. The executors
     * belong to the instance of the job, so stopping it (see {@link #shutdown}) stops them and nothing else.
     */
    private final ExecutorService workers = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("speaking-job-", 0).factory());
    private final ExecutorService analyses = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("speaking-analyse-", 0).factory());
    /** Keeps the rows of running submissions fresh, so a slow one is not taken over by another worker as stale. */
    private final ScheduledExecutorService heartbeats = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "speaking-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    /** Set once the instance is stopping: nothing new is claimed. */
    private volatile boolean stopping;
    /** The submissions this instance is grading right now, so the ones that do not finish before it stops can be handed back. */
    private final Set<UUID> running = ConcurrentHashMap.newKeySet();
    private long shutdownWaitSeconds = SHUTDOWN_WAIT_SECONDS;

    private final SpeakingSubmissionRepository submissions;
    private final ObjectStorage storage;
    private final SpeechAnalysisClient speech;
    private final SpeakingGrader grader;
    private final LlmUsageQuota quota;
    private final SpeakingPracticeSettings settings;
    private final ObjectMapper json;
    private final SpeakingGradingContextCapture contexts;
    private final SpeakingHistorySummarizer history;
    private final Clock clock = Clock.systemUTC();
    /** One permit per worker this instance may run. */
    private final Semaphore workerSlots;
    /** Optional: without a registry the job still grades, it only stops recording stage timers. */
    private MeterRegistry metrics;

    public SpeakingGradingJob(SpeakingSubmissionRepository submissions, @Lazy ObjectStorage storage,
                              SpeechAnalysisClient speech, SpeakingGrader grader, LlmUsageQuota quota,
                              SpeakingPracticeSettings settings,
                              ObjectMapper json, SpeakingGradingContextCapture contexts, SpeakingHistorySummarizer history) {
        this.contexts = contexts;
        this.history = history;
        this.submissions = submissions;
        this.storage = storage;
        this.speech = speech;
        this.grader = grader;
        this.quota = quota;
        this.settings = settings;
        this.json = json;
        this.workerSlots = new Semaphore(settings.gradingWorkers());
    }

    @Autowired(required = false)
    void setMetrics(MeterRegistry metrics) { this.metrics = metrics; }

    /** The safety net: starts workers for any queued, retryable or stalled submission a wake-up did not reach. */
    @Scheduled(fixedDelayString = "${learning.speaking.job-delay-ms:5000}")
    public void poll() {
        drain();
    }

    /** Claims one submission and grades it on the calling thread (the workers do the same on their own threads). */
    public void runOne() {
        if (!settings.enabled()) return;
        claim().ifPresent(this::process);
    }

    @Override
    public void wake() {
        if (!settings.enabled() || !settings.wakeOnSubmit() || stopping) return;
        try {
            workers.execute(this::drain);
        } catch (RuntimeException rejected) {
            // Best effort: the periodic poll finds the submission anyway.
            log.warn("Speaking grading wake-up not started: reason={}", rejected.getClass().getSimpleName());
        }
    }

    /**
     * Starts a worker for every free slot while there is a submission to claim, and returns at once. A worker that
     * finishes calls this again, so a queue is worked off without waiting for the next poll.
     */
    void drain() {
        if (!settings.enabled() || stopping) return;
        while (workerSlots.tryAcquire()) {
            Optional<SpeakingSubmission> next;
            try {
                next = claim();
            } catch (RuntimeException failure) {
                workerSlots.release();
                log.warn("Speaking submission could not be claimed: reason={}", failure.getClass().getSimpleName());
                return;
            }
            if (next.isEmpty()) {
                workerSlots.release();
                return;
            }
            SpeakingSubmission claimed = next.get();
            running.add(claimed.id());
            try {
                workers.execute(() -> work(claimed));
            } catch (RejectedExecutionException rejected) {
                // The claim stays on the row until it goes stale, then another poll takes it over.
                running.remove(claimed.id());
                workerSlots.release();
                throw rejected;
            }
        }
    }

    private void work(SpeakingSubmission submission) {
        running.add(submission.id());
        try {
            process(submission);
        } finally {
            running.remove(submission.id());
            workerSlots.release();
        }
        drain();
    }

    /**
     * Stops the instance without leaving a submission stuck. Nothing new is claimed; the submissions being graded get
     * {@link #SHUTDOWN_WAIT_SECONDS} to finish. A submission that is still running after that is handed back: it becomes
     * retryable at once and the claim does not count as an attempt, so a deploy does not use up the three attempts of a learner's
     * submission and does not leave it waiting for the stale limit.
     */
    @PreDestroy
    public void shutdown() {
        stopping = true;
        workers.shutdown();
        boolean finished = false;
        try {
            finished = workers.awaitTermination(shutdownWaitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (!finished) {
            for (UUID id : List.copyOf(running)) handBack(id);
        }
        workers.shutdownNow();
        analyses.shutdownNow();
        heartbeats.shutdownNow();
    }

    /** Best effort: whatever cannot be saved here is taken over by another instance once the row is stale. */
    private void handBack(UUID id) {
        try {
            submissions.findById(id).ifPresent(row -> {
                if (row.status() != SpeakingSubmissionStatus.ANALYZING && row.status() != SpeakingSubmissionStatus.GRADING) return;
                row.deferred("INSTANCE_STOPPED", Instant.now(clock));
                submissions.save(row);
                log.warn("Speaking grading handed back, the instance is stopping: submissionId={}", id);
            });
        } catch (RuntimeException failure) {
            log.warn("Speaking grading could not be handed back: submissionId={}, reason={}", id, failure.getClass().getSimpleName());
        }
    }

    /** For a test of the shutdown: how long it waits for the submissions being graded. */
    void shutdownWaitSeconds(long seconds) {
        this.shutdownWaitSeconds = seconds;
    }

    private Optional<SpeakingSubmission> claim() {
        Instant now = Instant.now(clock);
        return submissions.claimNext(now.minusSeconds(settings.staleAfterSeconds()),
                now.minusSeconds(settings.retryDelaySeconds()), settings.maxAttempts());
    }

    private void process(SpeakingSubmission submission) {
        LocalDate quotaDay = null;
        boolean graded = false;
        long begun = System.nanoTime();
        long analysisNanos = 0;
        long gradingNanos = 0;
        long queueWaitMillis = submission.queuedAt() == null ? -1
                : Math.max(0, Duration.between(submission.queuedAt(), Instant.now(clock)).toMillis());
        ScheduledFuture<?> heartbeat = startHeartbeat(submission.id());
        // Every log line of this thread, including the model calls, names the submission.
        MDC.put("submissionId", submission.id().toString());
        try {
            if (submission.result() != null) {
                // Already graded (an earlier run stopped before the status was saved): keep the grade, never grade twice.
                if (submission.status() != SpeakingSubmissionStatus.PAYMENT_PENDING) {
                    submission.paymentPending(submission.result(), submission.overallBand(), Instant.now(clock));
                    submissions.save(submission);
                }
                return;
            }
            if (!grader.available()) {
                failRetryable(submission, "LLM_UNAVAILABLE");
                return;
            }
            if (!speech.available()) {
                failRetryable(submission, "SPEECH_UNAVAILABLE");
                return;
            }
            List<SpeakingRecording> recordings = submission.recordings();
            long analysisBegun = System.nanoTime();
            List<SpeechAnalysis> analyses = analyze(submission, recordings);
            analysisNanos = System.nanoTime() - analysisBegun;
            if (analyses.stream().allMatch(SpeechAnalysis::isEmpty)) {
                // Nothing was heard, so there is nothing to grade and nothing to charge.
                submission.noSpeech(Instant.now(clock));
                submissions.save(submission);
                log.warn("Speaking submission holds no speech: submissionId={}", submission.id());
                return;
            }
            SpeakingGradingContext context = contexts.read(submissions.findGradingContext(submission.id()).orElse(null));
            SpeakingGradingInput input = input(submission, recordings, analyses, context);
            SpeakingPromptAssembler.Prepared prepared = grader.prepare(readRubricSnapshot(submission), input);
            quotaDay = quota(submission.userId());
            submission.beginGrading(prepared.rubric().code(), prepared.rubric().version(), prepared.checksum(),
                    SpeakingPromptAssembler.PROMPT_VERSION, Instant.now(clock));
            submissions.save(submission);
            long gradingBegun = System.nanoTime();
            SpeakingResultAssembler.Assembled result = grader.grade(prepared, input);
            gradingNanos = System.nanoTime() - gradingBegun;
            // The grade is kept as PAYMENT_PENDING and the learner's own request charges it (SettleSpeakingSubmissionUseCase).
            submission.paymentPending(result.result(), result.overallBand(), Instant.now(clock));
            submissions.save(submission);
            graded = true;
            recordTimings(submission, recordings.size(), queueWaitMillis, analysisNanos, gradingNanos, System.nanoTime() - begun);
        } catch (SpeakingPromptAssembler.TranscriptTooLongException exception) {
            needsExaminer(submission, "TRANSCRIPT_TOO_LONG");
        } catch (SpeakingPromptAssembler.UnusableRubricException exception) {
            needsExaminer(submission, "RUBRIC_UNUSABLE");
        } catch (SpeakingResultValidator.InvalidSpeakingGradeException exception) {
            needsExaminer(submission, "INVALID_GRADE");
        } catch (SpeechUnavailableException exception) {
            if (exception.isRetryable()) failRetryable(submission, "SPEECH_UNAVAILABLE");
            else needsExaminer(submission, "SPEECH_RESULT_INVALID");
        } catch (LlmUnavailableException exception) {
            failRetryable(submission, "LLM_UNAVAILABLE");
        } catch (DailyLimitException exception) {
            // The day's limit is not a grading failure: the claim does not count as an attempt.
            submission.deferred("DAILY_LIMIT_REACHED", Instant.now(clock));
            submissions.save(submission);
            log.warn("Speaking grading deferred: submissionId={}, code={}", submission.id(), "DAILY_LIMIT_REACHED");
        } catch (RuntimeException exception) {
            // The type and the place of the failure are enough to find the cause; the message could carry the learner's words.
            StackTraceElement origin = exception.getStackTrace().length == 0 ? null : exception.getStackTrace()[0];
            log.warn("Speaking grading failed: submissionId={}, code={}, exception={}, at={}", submission.id(),
                    "INTERNAL_FAILURE", exception.getClass().getName(),
                    origin == null ? "-" : origin.getClassName() + "." + origin.getMethodName() + ":" + origin.getLineNumber());
            failRetryable(submission, "INTERNAL_FAILURE");
        } finally {
            heartbeat.cancel(false);
            MDC.remove("submissionId");
            // A call that produced no grade does not use up the learner's day.
            if (quotaDay != null && !graded) releaseQuota(submission.userId(), quotaDay);
        }
    }

    /** How often a running submission's row is touched: a third of the stale limit, so it is never taken over while worked on. */
    static long heartbeatSeconds(int staleAfterSeconds) {
        return Math.max(5, staleAfterSeconds / 3);
    }

    private ScheduledFuture<?> startHeartbeat(UUID submissionId) {
        long seconds = heartbeatSeconds(settings.staleAfterSeconds());
        return heartbeats.scheduleAtFixedRate(() -> {
            try {
                submissions.touch(submissionId);
            } catch (RuntimeException failure) {
                log.warn("Speaking submission could not be kept fresh: submissionId={}, reason={}", submissionId,
                        failure.getClass().getSimpleName());
            }
        }, seconds, seconds, TimeUnit.SECONDS);
    }

    /**
     * Where the time of one grading went. {@code sinceCreatedMs} runs from the moment the learner's browser opened the
     * submission (so it holds the uploads) and {@code sinceQueuedMs} from the moment the last recording was in and the
     * submission was completed; both end when the grade is stored, before the learner's own request releases it. It names
     * no learner and carries no words; the numbers tell what to speed up next.
     */
    private void recordTimings(SpeakingSubmission submission, int recordings, long queueWaitMillis, long analysisNanos,
                               long gradingNanos, long totalNanos) {
        Instant now = Instant.now(clock);
        long sinceCreatedMillis = submission.createdAt() == null ? -1
                : Math.max(0, Duration.between(submission.createdAt(), now).toMillis());
        long sinceQueuedMillis = submission.queuedAt() == null ? -1
                : Math.max(0, Duration.between(submission.queuedAt(), now).toMillis());
        log.info("Speaking submission graded: submissionId={}, recordings={}, sinceCreatedMs={}, sinceQueuedMs={}, queueWaitMs={}, analysisMs={}, gradingMs={}, totalMs={}",
                submission.id(), recordings, sinceCreatedMillis, sinceQueuedMillis, queueWaitMillis,
                TimeUnit.NANOSECONDS.toMillis(analysisNanos), TimeUnit.NANOSECONDS.toMillis(gradingNanos),
                TimeUnit.NANOSECONDS.toMillis(totalNanos));
        if (metrics == null) return;
        if (sinceCreatedMillis >= 0) stageTimer("since_created").record(Duration.ofMillis(sinceCreatedMillis));
        if (queueWaitMillis >= 0) stageTimer("queue_wait").record(Duration.ofMillis(queueWaitMillis));
        stageTimer("analysis").record(Duration.ofNanos(analysisNanos));
        stageTimer("grading").record(Duration.ofNanos(gradingNanos));
        stageTimer("total").record(Duration.ofNanos(totalNanos));
    }

    private Timer stageTimer(String stage) {
        return Timer.builder("learning.speaking.stage").tag("stage", stage).register(metrics);
    }

    /**
     * The analyses of all recordings, in the order of the recordings. They run at the same time ({@code analysisParallelism}
     * at once, and the Azure client's own cap bounds all analyses together). Every one is waited for, so none is left running
     * unseen, and each finished analysis is already stored: when one fails the others are not done again on the retry.
     */
    private List<SpeechAnalysis> analyze(SpeakingSubmission submission, List<SpeakingRecording> recordings) {
        Semaphore atOnce = new Semaphore(settings.analysisParallelism());
        List<CompletableFuture<SpeechAnalysis>> started = new ArrayList<>();
        for (SpeakingRecording recording : recordings) {
            started.add(CompletableFuture.supplyAsync(() -> analyzeOne(submission, recording, atOnce), analyses));
        }
        List<SpeechAnalysis> analyses = new ArrayList<>();
        RuntimeException failure = null;
        for (CompletableFuture<SpeechAnalysis> future : started) {
            try {
                analyses.add(future.join());
            } catch (CompletionException exception) {
                RuntimeException cause = exception.getCause() instanceof RuntimeException runtime ? runtime : exception;
                if (failure == null) failure = cause; else failure.addSuppressed(cause);
            }
        }
        if (failure != null) throw failure;
        return analyses;
    }

    private SpeechAnalysis analyzeOne(SpeakingSubmission submission, SpeakingRecording recording, Semaphore atOnce) {
        atOnce.acquireUninterruptibly();
        try {
            String cached = submissions.findAnalysis(submission.id(), recording.questionVersionId()).orElse(null);
            SpeechAnalysis analysis;
            if (cached != null) {
                try { analysis = json.readValue(cached, SpeechAnalysis.class); }
                catch (Exception exception) { throw new IllegalStateException("Stored speech analysis is invalid", exception); }
            } else {
                byte[] audio = storage.download(settings.bucket(), recording.objectKey());
                long begun = System.nanoTime();
                try {
                    analysis = speech.analyze(audio, null);
                } catch (RuntimeException failure) {
                    // How long Azure took before it failed or timed out; the code says why.
                    logSpeechTiming(submission, recording, System.nanoTime() - begun,
                            failure instanceof SpeechUnavailableException ? failure.getMessage() : failure.getClass().getSimpleName());
                    throw failure;
                }
                logSpeechTiming(submission, recording, System.nanoTime() - begun, "ok");
                try { submissions.saveAnalysis(submission.id(), recording.questionVersionId(), json.writeValueAsString(analysis)); }
                catch (Exception exception) { throw new IllegalStateException("Speech analysis is not serializable", exception); }
            }
            // A long submission must not look stuck to another instance while it is still being worked on.
            submissions.touch(submission.id());
            return analysis;
        } finally {
            atOnce.release();
        }
    }

    /**
     * How long Azure took for how much audio: the ratio tells whether analysing is close to real time. {@code outcome} is
     * {@code ok} or the failure code (SPEECH_TIMEOUT, SPEECH_BUSY...), so a timeout shows how long it waited.
     */
    private void logSpeechTiming(SpeakingSubmission submission, SpeakingRecording recording, long nanos, String outcome) {
        log.info("Speech analysis done: submissionId={}, part={}, audioSeconds={}, analysisMs={}, outcome={}", submission.id(),
                recording.partNo(), recording.durationMs() == null ? "-" : String.format(java.util.Locale.ROOT, "%.1f", recording.durationMs() / 1000.0),
                TimeUnit.NANOSECONDS.toMillis(nanos), outcome);
    }

    private LearningContentClient.Rubric readRubricSnapshot(SpeakingSubmission submission) {
        if (submission.rubricSnapshot() == null) throw new SpeakingPromptAssembler.UnusableRubricException();
        return json.convertValue(submission.rubricSnapshot(), LearningContentClient.Rubric.class);
    }

    /** The answers in order, how far the transcript can be trusted, the learner's standing and the codes the model may name. */
    private SpeakingGradingInput input(SpeakingSubmission submission, List<SpeakingRecording> recordings,
                                       List<SpeechAnalysis> analyses, SpeakingGradingContext context) {
        List<SpeakingGradingInput.Answer> answers = new ArrayList<>();
        for (int index = 0; index < recordings.size(); index++) {
            SpeakingRecording recording = recordings.get(index);
            answers.add(new SpeakingGradingInput.Answer(recording.partNo(), recording.questionNo(),
                    recording.questionText(), analyses.get(index)));
        }
        List<SpeakingGradingInput.ErrorOption> errorCodes = context.errorTypes();
        Set<String> codes = errorCodes.stream().map(SpeakingGradingInput.ErrorOption::code).collect(Collectors.toSet());
        return new SpeakingGradingInput(submission.scope(), answers, TranscriptQuality.of(analyses),
                SpeakingAnalysisSummary.of(analyses, PronunciationBandMapper.INTERIM, 10),
                history.summarize(submission.userId(), codes, context.knowledgePoints()), errorCodes,
                settings.maxInputChars());
    }

    /** Takes one unit of the learner's day; returns the day, so the same unit can be given back. */
    private LocalDate quota(UUID userId) {
        LocalDate date = LocalDate.now(clock.withZone(ZoneId.of(settings.timezone())));
        if (!quota.tryConsume(userId, date, USAGE_KIND, settings.dailyGradingLimit())) {
            throw new DailyLimitException();
        }
        return date;
    }

    private void releaseQuota(UUID userId, LocalDate day) {
        try {
            quota.release(userId, day, USAGE_KIND);
        } catch (RuntimeException exception) {
            log.warn("Speaking quota could not be given back: reason={}", exception.getClass().getSimpleName());
        }
    }

    private void failRetryable(SpeakingSubmission submission, String code) {
        submission.retryable(code, Instant.now(clock));
        submissions.save(submission);
        log.warn("Speaking grading deferred: submissionId={}, code={}", submission.id(), code);
    }

    private void needsExaminer(SpeakingSubmission submission, String code) {
        submission.needsExaminer(code, Instant.now(clock));
        submissions.save(submission);
        log.warn("Speaking grade requires examiner: submissionId={}, code={}", submission.id(), code);
    }

    private static class DailyLimitException extends RuntimeException { }
}

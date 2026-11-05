package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.exception.LlmUnavailableException;
import com.ieltspath.learning.application.exception.SpeechUnavailableException;
import com.ieltspath.learning.application.port.LlmUsageQuota;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingGrader;
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
import com.ieltspath.learning.application.port.LearningContentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Claims and grades one submission at a time. Audio and LLM calls run outside database transactions. */
@Component
@EnableScheduling
public class SpeakingGradingJob {
    private static final Logger log = LoggerFactory.getLogger(SpeakingGradingJob.class);
    private static final String USAGE_KIND = "speaking_grading";

    private final SpeakingSubmissionRepository submissions;
    private final ObjectStorage storage;
    private final SpeechAnalysisClient speech;
    private final SpeakingGrader grader;
    private final LlmUsageQuota quota;
    private final SpeakingPracticeSettings settings;
    private final ObjectMapper json;
    private final LearningContentClient content;
    private final SpeakingHistorySummarizer history;
    private final Clock clock = Clock.systemUTC();

    public SpeakingGradingJob(SpeakingSubmissionRepository submissions, @Lazy ObjectStorage storage,
                              SpeechAnalysisClient speech, SpeakingGrader grader, LlmUsageQuota quota,
                              SpeakingPracticeSettings settings,
                              ObjectMapper json, LearningContentClient content, SpeakingHistorySummarizer history) {
        this.content = content;
        this.history = history;
        this.submissions = submissions;
        this.storage = storage;
        this.speech = speech;
        this.grader = grader;
        this.quota = quota;
        this.settings = settings;
        this.json = json;
    }

    @Scheduled(fixedDelayString = "${learning.speaking.job-delay-ms:5000}")
    public void runOne() {
        if (!settings.enabled()) return;
        submissions.claimNext(Instant.now(clock).minusSeconds(settings.staleAfterSeconds()), settings.maxAttempts())
                .ifPresent(this::process);
    }

    private void process(SpeakingSubmission submission) {
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
            List<SpeechAnalysis> analyses = analyze(submission, recordings);
            SpeakingGradingInput input = input(submission, recordings, analyses);
            SpeakingPromptAssembler.Prepared prepared = grader.prepare(readRubricSnapshot(submission), input);
            quota(submission.userId());
            submission.beginGrading(prepared.rubric().code(), prepared.rubric().version(), prepared.checksum(),
                    SpeakingPromptAssembler.PROMPT_VERSION, Instant.now(clock));
            submissions.save(submission);
            SpeakingResultAssembler.Assembled graded = grader.grade(prepared, input);
            // The grade is kept as PAYMENT_PENDING and the learner's own request charges it (SettleSpeakingSubmissionUseCase).
            submission.paymentPending(graded.result(), graded.overallBand(), Instant.now(clock));
            submissions.save(submission);
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
            failRetryable(submission, "DAILY_LIMIT_REACHED");
        } catch (RuntimeException exception) {
            log.warn("Speaking grading failed: submissionId={}, code={}", submission.id(), "INTERNAL_FAILURE");
            failRetryable(submission, "INTERNAL_FAILURE");
        }
    }

    private List<SpeechAnalysis> analyze(SpeakingSubmission submission, List<SpeakingRecording> recordings) {
        List<SpeechAnalysis> analyses = new ArrayList<>();
        for (SpeakingRecording recording : recordings) {
            String cached = submissions.findAnalysis(submission.id(), recording.questionVersionId()).orElse(null);
            SpeechAnalysis analysis;
            if (cached != null) {
                try { analysis = json.readValue(cached, SpeechAnalysis.class); }
                catch (Exception exception) { throw new IllegalStateException("Stored speech analysis is invalid", exception); }
            } else {
                byte[] audio = storage.download(settings.bucket(), recording.objectKey());
                analysis = speech.analyze(audio, null);
                try { submissions.saveAnalysis(submission.id(), recording.questionVersionId(), json.writeValueAsString(analysis)); }
                catch (Exception exception) { throw new IllegalStateException("Speech analysis is not serializable", exception); }
            }
            analyses.add(analysis);
        }
        return analyses;
    }

    private LearningContentClient.Rubric readRubricSnapshot(SpeakingSubmission submission) {
        if (submission.rubricSnapshot() == null) throw new SpeakingPromptAssembler.UnusableRubricException();
        return json.convertValue(submission.rubricSnapshot(), LearningContentClient.Rubric.class);
    }

    /** The answers in order, how far the transcript can be trusted, the learner's standing and the codes the model may name. */
    private SpeakingGradingInput input(SpeakingSubmission submission, List<SpeakingRecording> recordings,
                                       List<SpeechAnalysis> analyses) {
        List<SpeakingGradingInput.Answer> answers = new ArrayList<>();
        for (int index = 0; index < recordings.size(); index++) {
            SpeakingRecording recording = recordings.get(index);
            answers.add(new SpeakingGradingInput.Answer(recording.partNo(), recording.questionNo(),
                    recording.questionText(), analyses.get(index)));
        }
        List<SpeakingGradingInput.ErrorOption> errorCodes = errorOptions();
        Set<String> codes = errorCodes.stream().map(SpeakingGradingInput.ErrorOption::code).collect(Collectors.toSet());
        return new SpeakingGradingInput(submission.scope(), answers, TranscriptQuality.of(analyses),
                SpeakingAnalysisSummary.of(analyses, PronunciationBandMapper.INTERIM, 10),
                history.summarize(submission.userId(), codes), errorCodes, settings.maxInputChars());
    }

    /** Active Speaking error types; a Content outage only means the grade carries no error list. */
    private List<SpeakingGradingInput.ErrorOption> errorOptions() {
        try {
            return content.activeErrorTypes().stream().filter(type -> type.code() != null && type.code().startsWith("SPK_"))
                    .map(type -> new SpeakingGradingInput.ErrorOption(type.code(), type.name())).toList();
        } catch (RuntimeException unavailable) {
            log.warn("Speaking error types unavailable: {}", unavailable.getClass().getSimpleName());
            return List.of();
        }
    }

    private void quota(UUID userId) {
        LocalDate date = LocalDate.now(clock.withZone(ZoneId.of(settings.timezone())));
        if (!quota.tryConsume(userId, date, USAGE_KIND, settings.dailyGradingLimit())) {
            throw new DailyLimitException();
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

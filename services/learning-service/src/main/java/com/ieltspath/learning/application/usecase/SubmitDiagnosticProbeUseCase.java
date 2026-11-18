package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.result.DiagnosticProbeSubmissionResult;
import com.ieltspath.learning.application.result.SubmissionResult;
import com.ieltspath.learning.application.service.AnswerDurations;
import com.ieltspath.learning.application.service.DiagnosticFlow;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.application.service.ErrorTrapEvidence;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.application.service.DiagnosticPreparation;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.AnswerSpecGrader;
import com.ieltspath.learning.domain.service.PassMark;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Grades one probe. Its answers count as mastery evidence of the probed point, the solutions are shown afterwards, and
 * a failed probe opens the probes of its own prerequisites. Sending the same {@code requestId} again returns the same
 * result without writing anything.
 */
@Service
public class SubmitDiagnosticProbeUseCase {
    private final LearnerLock lock;
    private final DiagnosticRepository diagnostics;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final LearningContentClient content;
    private final KnowledgeEvidenceRepository evidence;
    private final QuestionEventLog questionEvents;
    private final MasteryProfileUpdater masteryProfiles;
    private final DiagnosticFlow flow;
    private final ErrorTrapEvidence errorTrapEvidence;
    private final DiagnosticPreparation preparation;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final ItemGrading grading = new ItemGrading();

    public SubmitDiagnosticProbeUseCase(LearnerLock lock, DiagnosticRepository diagnostics,
                                        PracticeAttemptRepository attempts, ReviewItemRepository reviews,
                                        LearningContentClient content,
                                        KnowledgeEvidenceRepository evidence, QuestionEventLog questionEvents,
                                        MasteryProfileUpdater masteryProfiles, DiagnosticFlow flow,
                                        ErrorTrapEvidence errorTrapEvidence, DiagnosticPreparation preparation,
                                        ObjectMapper json, PlatformTransactionManager transactionManager) {
        this.lock = lock;
        this.diagnostics = diagnostics;
        this.attempts = attempts;
        this.reviews = reviews;
        this.content = content;
        this.evidence = evidence;
        this.questionEvents = questionEvents;
        this.masteryProfiles = masteryProfiles;
        this.flow = flow;
        this.errorTrapEvidence = errorTrapEvidence;
        this.preparation = preparation;
        this.json = json;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public DiagnosticProbeSubmissionResult execute(UUID userId, UUID sessionId, UUID probeId,
                                                   SubmitExerciseCommand command) {
        for (int retry = 0; retry < 2; retry++) {
            diagnostics.findSession(userId, sessionId).orElseThrow(
                    () -> new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found"));
            DiagnosticProbe owned = diagnostics.findProbe(sessionId, probeId).orElseThrow(
                    () -> new LearningRequestException(404, "NOT_FOUND", "Diagnostic probe was not found"));
            boolean replay = owned.status() == DiagnosticProbe.Status.ANSWERED
                    && command.requestId().equals(owned.requestId());
            if (replay) {
                var stored = diagnostics.probeResponse(owned.id());
                if (stored.isPresent()) return readResponse(stored.get());
            }
            if (owned.status() != DiagnosticProbe.Status.OPEN && !replay) {
                throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");
            }
            if (!replay && attempts.revealedPackageIds(userId).contains(owned.packageId())) {
                throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");
            }
            PackageVersion version;
            try {
                version = content.getPackageVersion(owned.packageVersionId());
            } catch (LearningRequestException error) {
                if (error.getStatus() != 404 || !"NOT_FOUND".equals(error.getCode()) || replay) throw error;
                throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is no longer available");
            }
            var graded = grading.grade(version, command.answers(), "Diagnostic probe");
            ErrorTrapCatalog.ErrorTraps trapCatalog = errorTrapEvidence.loadIfNeeded(
                    graded.items().stream().anyMatch(item -> item.answerSpec().get("distractors") instanceof Map<?, ?>));
            Map<String, com.ieltspath.learning.application.service.ErrorTypeTexts.Text> errorTexts =
                    errorTrapEvidence.errorTexts(graded.errorCodes());
            DiagnosticFlow.Prepared diagnosticData = graded.passed()
                    ? preparation.describeOnly() : preparation.forSources(userId, List.of(owned.knowledgePointId()));
            ProbeSubmissionRead read = transaction.execute(status -> submitUnderLock(userId, sessionId, probeId,
                    command, owned, version, graded, trapCatalog, errorTexts, diagnosticData));
            if (!read.retry()) return read.result();
        }
        throw new LearningRequestException(409, "PROBE_CLOSED", "Probe changed while it was being submitted");
    }

    private ProbeSubmissionRead submitUnderLock(UUID userId, UUID sessionId, UUID probeId,
                                               SubmitExerciseCommand command, DiagnosticProbe preparedProbe,
                                               PackageVersion version, ItemGrading.Graded graded,
                                               ErrorTrapCatalog.ErrorTraps trapCatalog,
                                               Map<String, com.ieltspath.learning.application.service.ErrorTypeTexts.Text> errorTexts,
                                               DiagnosticFlow.Prepared diagnosticData) {
        lock.lock(userId);
        diagnostics.findSession(userId, sessionId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found"));
        DiagnosticProbe probe = diagnostics.findProbe(sessionId, probeId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Diagnostic probe was not found"));
        boolean replay = probe.status() == DiagnosticProbe.Status.ANSWERED
                && command.requestId().equals(probe.requestId());
        if (replay) {
            var stored = diagnostics.probeResponse(probe.id());
            if (stored.isPresent()) return ProbeSubmissionRead.done(readResponse(stored.get()));
        }
        if (replay && !preparedProbe.packageVersionId().equals(probe.packageVersionId())) {
            return ProbeSubmissionRead.retryRead();
        }
        if (!replay && (probe.status() != DiagnosticProbe.Status.OPEN
                || !java.util.Objects.equals(probe.packageVersionId(), preparedProbe.packageVersionId()))) {
            return ProbeSubmissionRead.retryRead();
        }
        if (!replay && attempts.revealedPackageIds(userId).contains(probe.packageId())) {
            throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");
        }
        if (!replay && candidatesBecameStale(userId, diagnosticData)) return ProbeSubmissionRead.retryRead();
        if (!replay) record(userId, probe, command, graded, trapCatalog, diagnosticData);
        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        for (int index = 0; index < graded.items().size(); index++) {
            Item item = graded.items().get(index);
            AnswerSpecGrader.Grade grade = graded.grades().get(index);
            results.add(new SubmissionResult.AnswerResult(item.questionVersionId(), grade.correct(),
                    grade.correctAnswer(), item.explanation(), null,
                    errorTrapEvidence.feedback(graded.errorCodes().get(index), errorTexts)));
        }
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        String transcript = first != null && first.audio() != null ? first.audio().transcript() : null;
        DiagnosticSession current = diagnostics.findSession(userId, sessionId).orElseThrow();
        DiagnosticProbeSubmissionResult result = new DiagnosticProbeSubmissionResult(probeId,
                PassMark.passes(graded.correct(), graded.total()),
                graded.correct(), graded.total(), List.copyOf(results), transcript, current.status().name(),
                current.conclusionKnowledgePointIds());
        if (!replay) saveResponse(probeId, result);
        return ProbeSubmissionRead.done(result);
    }

    private boolean candidatesBecameStale(UUID userId, DiagnosticFlow.Prepared prepared) {
        Set<UUID> assigned = new HashSet<>(attempts.revealedPackageIds(userId));
        assigned.addAll(reviews.assignedPackageIds(userId));
        return prepared.candidates().values().stream().anyMatch(candidates -> !candidates.isEmpty()
                && candidates.stream().allMatch(candidate -> assigned.contains(candidate.packageId())));
    }

    private void record(UUID userId, DiagnosticProbe probe, SubmitExerciseCommand command, ItemGrading.Graded graded,
                        ErrorTrapCatalog.ErrorTraps trapCatalog, DiagnosticFlow.Prepared diagnosticData) {
        UUID probedPoint = probe.knowledgePointId();
        Map<UUID, Long> durations = AnswerDurations.of(command.answers());
        List<KnowledgeEvidence> answers = new ArrayList<>();
        List<QuestionAttemptEvent> events = new ArrayList<>();
        java.util.Set<UUID> withoutMilestone = new java.util.HashSet<>();
        for (int index = 0; index < graded.items().size(); index++) {
            Item item = graded.items().get(index);
            boolean correct = graded.grades().get(index).correct();
            List<UUID> mapped = item.knowledgePointMappings().stream()
                    .map(LearningContentClient.KnowledgePointMapping::knowledgePointId).distinct().toList();
            for (UUID kpId : new LinkedHashSet<>(mapped)) {
                answers.add(KnowledgeEvidence.of(kpId, correct, EvidenceSource.DIAGNOSTIC_PROBE,
                        LessonEvidenceReference.forDiagnosticProbe(command.requestId(), item.questionVersionId(),
                                kpId)).forQuestion(item.questionVersionId()));
            }
            errorTrapEvidence.append(answers, withoutMilestone, item.answerSpec(),
                    correct, graded.errorCodes().get(index), item.questionVersionId(),
                    kp -> LessonEvidenceReference.forDiagnosticProbe(command.requestId(), item.questionVersionId(), kp),
                    trapCatalog);
            events.add(QuestionAttemptEvent.of(userId, item.questionVersionId(), mapped, correct,
                    durations.get(item.questionVersionId()), graded.errorCodes().get(index),
                    QuestionEventSource.DIAGNOSTIC_PROBE, command.requestId(), true));
        }
        questionEvents.append(events);
        evidence.append(userId, answers);
        masteryProfiles.recordEvidence(userId, answers, withoutMilestone);
        errorTrapEvidence.createReviews(userId, null, answers);

        boolean passed = PassMark.passes(graded.correct(), graded.total());
        diagnostics.answerProbe(probe.id(), graded.correct(), graded.total(), passed, command.requestId());
        DiagnosticProbe answered = new DiagnosticProbe(probe.id(), probe.sessionId(), probedPoint,
                probe.parentKnowledgePointId(), probe.depth(), probe.packageId(), probe.packageVersionId(),
                DiagnosticProbe.Status.ANSWERED, graded.correct(), graded.total(), passed, command.requestId(), null);
        flow.afterAnswer(diagnostics.findSession(userId, probe.sessionId()).orElseThrow(), answered, passed,
                diagnosticData);
    }

    private record ProbeSubmissionRead(DiagnosticProbeSubmissionResult result, boolean retry) {
        static ProbeSubmissionRead done(DiagnosticProbeSubmissionResult result) {
            return new ProbeSubmissionRead(result, false);
        }
        static ProbeSubmissionRead retryRead() { return new ProbeSubmissionRead(null, true); }
    }

    private void saveResponse(UUID probeId, DiagnosticProbeSubmissionResult result) {
        try {
            diagnostics.saveProbeResponse(probeId, json.writeValueAsString(result));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot store diagnostic response", exception);
        }
    }

    private DiagnosticProbeSubmissionResult readResponse(String response) {
        try {
            return json.readValue(response, DiagnosticProbeSubmissionResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot read diagnostic response", exception);
        }
    }
}

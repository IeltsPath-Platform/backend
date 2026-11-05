package com.ieltspath.learning.application.usecase;

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
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.service.AnswerSpecGrader;
import com.ieltspath.learning.domain.service.PassMark;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final LearningContentClient content;
    private final KnowledgeEvidenceRepository evidence;
    private final QuestionEventLog questionEvents;
    private final MasteryProfileUpdater masteryProfiles;
    private final DiagnosticFlow flow;
    private final ErrorTrapEvidence errorTrapEvidence;
    private final ItemGrading grading = new ItemGrading();

    public SubmitDiagnosticProbeUseCase(LearnerLock lock, DiagnosticRepository diagnostics,
                                        PracticeAttemptRepository attempts, LearningContentClient content,
                                        KnowledgeEvidenceRepository evidence, QuestionEventLog questionEvents,
                                        MasteryProfileUpdater masteryProfiles, DiagnosticFlow flow,
                                        ErrorTrapEvidence errorTrapEvidence) {
        this.lock = lock;
        this.diagnostics = diagnostics;
        this.attempts = attempts;
        this.content = content;
        this.evidence = evidence;
        this.questionEvents = questionEvents;
        this.masteryProfiles = masteryProfiles;
        this.flow = flow;
        this.errorTrapEvidence = errorTrapEvidence;
    }

    @Transactional
    public DiagnosticProbeSubmissionResult execute(UUID userId, UUID sessionId, UUID probeId,
                                                   SubmitExerciseCommand command) {
        lock.lock(userId);
        diagnostics.findSession(userId, sessionId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found"));
        DiagnosticProbe probe = diagnostics.findProbe(sessionId, probeId).orElseThrow(
                () -> new LearningRequestException(404, "NOT_FOUND", "Diagnostic probe was not found"));
        boolean replay = probe.status() == DiagnosticProbe.Status.ANSWERED
                && command.requestId().equals(probe.requestId());
        if (probe.status() != DiagnosticProbe.Status.OPEN && !replay) {
            throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");
        }
        if (!replay && attempts.revealedPackageIds(userId).contains(probe.packageId())) {
            throw new LearningRequestException(409, "PROBE_CLOSED", "Diagnostic probe is closed");
        }
        PackageVersion version = content.getPackageVersion(probe.packageVersionId());
        var graded = grading.grade(version, command.answers(), "Diagnostic probe");
        if (!replay) record(userId, probe, command, graded);

        List<SubmissionResult.AnswerResult> results = new ArrayList<>();
        Map<String, com.ieltspath.learning.application.service.ErrorTypeTexts.Text> errorTexts =
                errorTrapEvidence.errorTexts(graded.errorCodes());
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
        return new DiagnosticProbeSubmissionResult(probeId, PassMark.passes(graded.correct(), graded.total()),
                graded.correct(), graded.total(), List.copyOf(results), transcript, current.status().name(),
                current.conclusionKnowledgePointIds());
    }

    private void record(UUID userId, DiagnosticProbe probe, SubmitExerciseCommand command, ItemGrading.Graded graded) {
        UUID probedPoint = probe.knowledgePointId();
        Map<UUID, Long> durations = AnswerDurations.of(command.answers());
        List<KnowledgeEvidence> answers = new ArrayList<>();
        List<QuestionAttemptEvent> events = new ArrayList<>();
        ErrorTrapCatalog.ErrorTraps trapCatalog = errorTrapEvidence.loadIfNeeded(
                graded.items().stream().anyMatch(item -> item.answerSpec().get("distractors") instanceof Map<?, ?>));
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
        flow.afterAnswer(diagnostics.findSession(userId, probe.sessionId()).orElseThrow(), answered, passed);
    }
}

package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.result.DiagnosticProbeResult;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.application.service.DiagnosticFlow;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The questions of an open probe. A set the learner has meanwhile seen in Practice or a review is swapped for another
 * unseen set, or the probe becomes NO_SET, so a probe never tests answers the learner already knows.
 */
@Service
public class GetDiagnosticProbeUseCase {
    private final LearnerLock lock;
    private final DiagnosticRepository diagnostics;
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final DiagnosticFlow flow;

    public GetDiagnosticProbeUseCase(LearnerLock lock, DiagnosticRepository diagnostics,
                                     PracticeAttemptRepository attempts, LearningContentClient content,
                                     DiagnosticFlow flow) {
        this.lock = lock;
        this.diagnostics = diagnostics;
        this.attempts = attempts;
        this.content = content;
        this.flow = flow;
    }

    @Transactional
    public DiagnosticProbeResult execute(UUID userId, UUID sessionId, UUID probeId) {
        lock.lock(userId);
        DiagnosticSession session = diagnostics.findSession(userId, sessionId)
                .orElseThrow(GetDiagnosticProbeUseCase::notFound);
        DiagnosticProbe probe = diagnostics.findProbe(sessionId, probeId)
                .orElseThrow(GetDiagnosticProbeUseCase::notFound);
        if (probe.status() == DiagnosticProbe.Status.OPEN
                && attempts.revealedPackageIds(userId).contains(probe.packageId())) {
            flow.replaceStaleSet(session, probe);
            probe = diagnostics.findProbe(sessionId, probeId).orElseThrow(GetDiagnosticProbeUseCase::notFound);
        }
        if (probe.status() != DiagnosticProbe.Status.OPEN) {
            return new DiagnosticProbeResult(sessionId, probeId, probe.knowledgePointId(), probe.depth(),
                    probe.status().name(), null, null, List.of());
        }
        PackageVersion version = content.getPackageVersion(probe.packageVersionId());
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        List<LessonResult.Question> questions = ItemGrading.orderedItems(version).stream()
                .map(item -> new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                        options(item.options()), item.hint())).toList();
        ReviewResult.Audio audio = first == null || first.audio() == null ? null
                : new ReviewResult.Audio(first.audio().mediaUrl(), first.audio().durationSeconds());
        return new DiagnosticProbeResult(sessionId, probeId, probe.knowledgePointId(), probe.depth(),
                probe.status().name(), first == null ? null : first.passage(), audio, questions);
    }

    private static List<LessonResult.Option> options(List<LearningContentClient.Option> options) {
        return options == null ? null : options.stream()
                .map(option -> new LessonResult.Option(option.optionKey(), option.content(), option.sortOrder()))
                .toList();
    }

    private static LearningRequestException notFound() {
        return new LearningRequestException(404, "NOT_FOUND", "Diagnostic probe was not found");
    }
}

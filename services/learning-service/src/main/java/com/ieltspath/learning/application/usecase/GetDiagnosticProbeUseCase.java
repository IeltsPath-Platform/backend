package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.service.DiagnosticPreparation;
import com.ieltspath.learning.application.result.DiagnosticProbeResult;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.application.service.DiagnosticFlow;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
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
    private final ReviewItemRepository reviews;
    private final DiagnosticPreparation preparation;
    private final TransactionTemplate transaction;

    public GetDiagnosticProbeUseCase(LearnerLock lock, DiagnosticRepository diagnostics,
                                     PracticeAttemptRepository attempts, LearningContentClient content,
                                     DiagnosticFlow flow, ReviewItemRepository reviews,
                                     DiagnosticPreparation preparation,
                                     PlatformTransactionManager transactionManager) {
        this.lock = lock;
        this.diagnostics = diagnostics;
        this.attempts = attempts;
        this.content = content;
        this.flow = flow;
        this.reviews = reviews;
        this.preparation = preparation;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public DiagnosticProbeResult execute(UUID userId, UUID sessionId, UUID probeId) {
        for (int retry = 0; retry < 2; retry++) {
            DiagnosticSession ownedSession = diagnostics.findSession(userId, sessionId)
                    .orElseThrow(GetDiagnosticProbeUseCase::notFound);
            DiagnosticProbe ownedProbe = diagnostics.findProbe(sessionId, probeId)
                    .orElseThrow(GetDiagnosticProbeUseCase::notFound);
            if (ownedProbe.status() != DiagnosticProbe.Status.OPEN) {
                return emptyResult(sessionId, probeId, ownedProbe);
            }
            boolean stale = attempts.revealedPackageIds(userId).contains(ownedProbe.packageId());
            PackageVersion version = null;
            if (!stale) {
                try {
                    version = content.getPackageVersion(ownedProbe.packageVersionId());
                } catch (LearningRequestException error) {
                    if (error.getStatus() != 404 || !"NOT_FOUND".equals(error.getCode())) throw error;
                    stale = true;
                }
            }
            DiagnosticPreparation.ReplacementData replacement = stale
                    ? preparation.replacementsFor(userId, ownedProbe.knowledgePointId()) : null;
            PackageVersion preparedVersion = version;
            boolean staleSet = stale;
            ProbeRead read = transaction.execute(status -> readUnderLock(userId, sessionId, probeId,
                    ownedProbe, preparedVersion, staleSet, replacement));
            if (!read.retry()) return render(sessionId, probeId, read.probe(), read.version());
        }
        throw new LearningRequestException(409, "PROBE_CLOSED", "Probe changed while it was being opened");
    }

    private ProbeRead readUnderLock(UUID userId, UUID sessionId, UUID probeId, DiagnosticProbe preparedProbe,
                                    PackageVersion preparedVersion, boolean stale,
                                    DiagnosticPreparation.ReplacementData replacement) {
        lock.lock(userId);
        DiagnosticSession session = diagnostics.findSession(userId, sessionId)
                .orElseThrow(GetDiagnosticProbeUseCase::notFound);
        DiagnosticProbe probe = diagnostics.findProbe(sessionId, probeId)
                .orElseThrow(GetDiagnosticProbeUseCase::notFound);
        if (!probe.id().equals(preparedProbe.id()) || probe.status() != preparedProbe.status()
                || !java.util.Objects.equals(probe.packageVersionId(), preparedProbe.packageVersionId())) {
            return ProbeRead.retryRead();
        }
        if (probe.status() != DiagnosticProbe.Status.OPEN) return ProbeRead.done(probe, null);
        Set<UUID> revealed = attempts.revealedPackageIds(userId);
        if (!stale && revealed.contains(probe.packageId())) return ProbeRead.retryRead();
        if (stale) {
            Set<UUID> assigned = new HashSet<>(revealed);
            assigned.addAll(reviews.assignedPackageIds(userId));
            diagnostics.findProbes(sessionId).stream().map(DiagnosticProbe::packageId)
                    .filter(java.util.Objects::nonNull).forEach(assigned::add);
            var selected = replacement.candidates().stream()
                    .filter(candidate -> !assigned.contains(candidate.set().packageId())).findFirst();
            if (replacement.candidates().size() > 0 && selected.isEmpty()) return ProbeRead.retryRead();
            flow.replaceStaleSet(session, probe, selected.map(DiagnosticPreparation.PreparedSet::set),
                    replacement.directory());
            probe = diagnostics.findProbe(sessionId, probeId).orElseThrow(GetDiagnosticProbeUseCase::notFound);
            return ProbeRead.done(probe, selected.map(DiagnosticPreparation.PreparedSet::version).orElse(null));
        }
        if (preparedVersion == null || !probe.packageVersionId().equals(preparedVersion.packageVersionId())) {
            return ProbeRead.retryRead();
        }
        return ProbeRead.done(probe, preparedVersion);
    }

    private static DiagnosticProbeResult render(UUID sessionId, UUID probeId, DiagnosticProbe probe,
                                                PackageVersion version) {
        if (probe.status() != DiagnosticProbe.Status.OPEN || version == null) return emptyResult(sessionId, probeId, probe);
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        List<LessonResult.Question> questions = ItemGrading.orderedItems(version).stream()
                .map(item -> new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                        options(item.options()), item.hint())).toList();
        ReviewResult.Audio audio = first == null || first.audio() == null ? null
                : new ReviewResult.Audio(first.audio().mediaUrl(), first.audio().durationSeconds());
        return new DiagnosticProbeResult(sessionId, probeId, probe.knowledgePointId(), probe.depth(),
                probe.status().name(), first == null ? null : first.passage(), audio, questions);
    }

    private static DiagnosticProbeResult emptyResult(UUID sessionId, UUID probeId, DiagnosticProbe probe) {
        return new DiagnosticProbeResult(sessionId, probeId, probe.knowledgePointId(), probe.depth(),
                probe.status().name(), null, null, List.of());
    }

    private record ProbeRead(DiagnosticProbe probe, PackageVersion version, boolean retry) {
        static ProbeRead done(DiagnosticProbe probe, PackageVersion version) { return new ProbeRead(probe, version, false); }
        static ProbeRead retryRead() { return new ProbeRead(null, null, true); }
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

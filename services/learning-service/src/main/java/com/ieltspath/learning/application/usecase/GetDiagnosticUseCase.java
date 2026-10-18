package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.DiagnosticResult;
import com.ieltspath.learning.application.result.DiagnosticResult.Point;
import com.ieltspath.learning.application.result.DiagnosticResult.ProbeRow;
import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A diagnosis with its probes as a matrix of knowledge point against result, and its conclusion once reached. */
@Service
public class GetDiagnosticUseCase {
    private final DiagnosticRepository diagnostics;
    private final KnowledgePointDirectory directory;

    public GetDiagnosticUseCase(DiagnosticRepository diagnostics, KnowledgePointDirectory directory) {
        this.diagnostics = diagnostics;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public DiagnosticResult execute(UUID userId, UUID sessionId) {
        DiagnosticSession session = diagnostics.findSession(userId, sessionId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Diagnosis was not found"));
        Map<UUID, KnowledgePointDirectory.Entry> described = directory.describe();
        List<ProbeRow> rows = diagnostics.findProbes(sessionId).stream().map(probe -> new ProbeRow(probe.id(),
                point(described, probe.knowledgePointId()), probe.parentKnowledgePointId(), probe.depth(),
                probe.status().name(), probe.correct(), probe.total(), outcome(probe))).toList();
        List<Point> conclusion = session.conclusionKnowledgePointIds().stream()
                .map(kp -> point(described, kp)).toList();
        boolean rootIsGap = session.conclusionKnowledgePointIds().size() == 1
                && session.conclusionKnowledgePointIds().contains(session.rootKnowledgePointId());
        return new DiagnosticResult(session.id(), session.rootReviewId(), session.rootKnowledgePointId(),
                session.status().name(), rows, conclusion, !session.probing() && rootIsGap,
                diagnostics.pendingWaitingOn(session.rootReviewId()));
    }

    private static Point point(Map<UUID, KnowledgePointDirectory.Entry> described, UUID kp) {
        KnowledgePointDirectory.Entry entry = described.getOrDefault(kp, KnowledgePointDirectory.Entry.UNKNOWN);
        return new Point(kp, entry.code(), entry.name());
    }

    private static String outcome(DiagnosticProbe probe) {
        return switch (probe.status()) {
            case OPEN -> null;
            case NO_SET -> "UNKNOWN";
            case ANSWERED -> Boolean.TRUE.equals(probe.passed()) ? "PASSED" : "FAILED";
        };
    }
}

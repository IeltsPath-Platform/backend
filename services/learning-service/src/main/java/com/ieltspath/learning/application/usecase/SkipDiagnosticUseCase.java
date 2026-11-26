package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.DiagnosticResult;
import com.ieltspath.learning.application.service.DiagnosticFlow;
import com.ieltspath.learning.application.service.DiagnosticPreparation;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/** Closes a diagnosis after at least one probe has been answered. */
@Service
public class SkipDiagnosticUseCase {
    private final LearnerLock lock;
    private final DiagnosticRepository diagnostics;
    private final DiagnosticPreparation preparation;
    private final DiagnosticFlow flow;
    private final GetDiagnosticUseCase getDiagnostic;
    private final TransactionTemplate transaction;

    public SkipDiagnosticUseCase(LearnerLock lock, DiagnosticRepository diagnostics,
                                 DiagnosticPreparation preparation, DiagnosticFlow flow,
                                 GetDiagnosticUseCase getDiagnostic,
                                 PlatformTransactionManager transactionManager) {
        this.lock = lock;
        this.diagnostics = diagnostics;
        this.preparation = preparation;
        this.flow = flow;
        this.getDiagnostic = getDiagnostic;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public DiagnosticResult execute(UUID userId, UUID sessionId) {
        diagnostics.findSession(userId, sessionId).orElseThrow(SkipDiagnosticUseCase::notFound);
        DiagnosticFlow.Prepared described = preparation.describeOnly();
        transaction.executeWithoutResult(status -> {
            lock.lock(userId);
            DiagnosticSession session = diagnostics.findSession(userId, sessionId)
                    .orElseThrow(SkipDiagnosticUseCase::notFound);
            if (!session.probing()) return;
            boolean answered = diagnostics.findProbes(sessionId).stream()
                    .anyMatch(probe -> probe.status() == DiagnosticProbe.Status.ANSWERED);
            if (!answered) {
                throw new LearningRequestException(409, "DIAGNOSIS_PROBE_REQUIRED",
                        "Vui lòng trả lời ít nhất một câu hỏi kiểm tra chẩn đoán trước khi bỏ qua.");
            }
            flow.skip(session, described.directory());
        });
        return getDiagnostic.execute(userId, sessionId);
    }

    private static LearningRequestException notFound() {
        return new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy phiên chẩn đoán.");
    }
}

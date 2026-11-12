package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.PracticeSessionResult;
import com.ieltspath.learning.domain.aggregate.PracticeSession;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import com.ieltspath.learning.domain.repository.PracticeSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

@Service
public class OpenPracticeSessionUseCase {
    private static final Set<String> KINDS = Set.of("WRITING", "SPEAKING", "VOCABULARY");
    private final PracticeSessionRepository sessions;
    private final LearnerLock learnerLock;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public OpenPracticeSessionUseCase(PracticeSessionRepository sessions, LearnerLock learnerLock,
            TransactionTemplate transactions, Clock clock) {
        this.sessions = sessions;
        this.learnerLock = learnerLock;
        this.transactions = transactions;
        this.clock = clock;
    }

    public PracticeSessionResult execute(UUID userId, String kind, UUID requestId) {
        if (!KINDS.contains(kind)) throw new IllegalArgumentException("Unsupported practice session kind");
        return transactions.execute(status -> {
            learnerLock.lock(userId);
            var previous = sessions.findByRequest(userId, requestId);
            if (previous.isPresent()) {
                var session = previous.get();
                if (!session.kind().equals(kind)) throw new StudyTrackingException("PAYLOAD_CONFLICT");
                return PracticeSessionResult.from(session);
            }
            var session = PracticeSession.start(userId, kind, requestId, clock.instant());
            sessions.save(session);
            return PracticeSessionResult.from(session);
        });
    }
}

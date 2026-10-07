package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.application.service.PracticeAttemptViewAssembler;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class StartPracticeAttemptUseCase {
    private final LearnerLock lock;
    private final PracticeAccess access;
    private final LearningContentClient content;
    private final PracticeAttemptRepository attempts;
    private final PracticeAttemptViewAssembler view;
    private final Clock clock = Clock.systemUTC();

    public StartPracticeAttemptUseCase(LearnerLock lock, PracticeAccess access, LearningContentClient content,
                                       PracticeAttemptRepository attempts, PracticeAttemptViewAssembler view) {
        this.lock = lock;
        this.access = access;
        this.content = content;
        this.attempts = attempts;
        this.view = view;
    }

    @Transactional
    public PracticeAttemptView execute(UUID userId, UUID lessonId, UUID packageId) {
        lock.lock(userId);
        var lesson = access.require(userId, lessonId);
        var set = content.lessonPracticeSets(lessonId).stream()
                .filter(item -> item.packageId().equals(packageId)).findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice set was not found"));
        PracticeAttempt attempt = attempts.findOpen(userId, packageId).orElse(null);
        if (attempt == null) {
            attempt = PracticeAttempt.start(UUID.randomUUID(), userId, lessonId,
                    set.skills().isEmpty() ? lesson.skills() : set.skills(), packageId,
                    set.packageVersionId(), clock.instant());
            attempts.insert(attempt);
        }
        return view.assemble(attempt, content.getPackageVersion(attempt.packageVersionId()));
    }
}

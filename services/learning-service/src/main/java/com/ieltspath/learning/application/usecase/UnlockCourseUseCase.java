package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.exception.InsufficientPointsException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.PendingCourseUnlockWriter;
import com.ieltspath.learning.application.result.CourseUnlockResult;
import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.exception.UnlockPricingException;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.service.UnlockPricing;
import com.ieltspath.learning.domain.vo.CourseUnlockTarget;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.application.exception.LearningRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UnlockCourseUseCase {
    private final LearnerLock lock;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final LearnerCourseUnlockRepository unlocks;
    private final PendingCourseUnlockWriter intentWriter;
    private final ReconcilePendingCourseUnlocks reconciler;
    private final UnlockPricing pricing = new UnlockPricing();

    public UnlockCourseUseCase(LearnerLock lock, RefreshLearningTopicsUseCase refreshTopics,
                               LearnerCourseUnlockRepository unlocks, PendingCourseUnlockWriter intentWriter,
                               ReconcilePendingCourseUnlocks reconciler) {
        this.lock = lock;
        this.refreshTopics = refreshTopics;
        this.unlocks = unlocks;
        this.intentWriter = intentWriter;
        this.reconciler = reconciler;
    }

    @Transactional
    public CourseUnlockResult execute(UUID userId, UUID courseId, String requestedTarget, UUID requestId) {
        if (requestId == null) throw new LearningRequestException(400, "IDEMPOTENCY_KEY_REQUIRED",
                "Idempotency-Key is required");
        CourseUnlockTarget target;
        try {
            target = CourseUnlockTarget.parse(requestedTarget);
        } catch (IllegalArgumentException exception) {
            throw new LearningRequestException(400, "INVALID_UNLOCK_TARGET", "Unsupported unlock target");
        }

        lock.lock(userId);
        var existingRequest = unlocks.findByRequestId(requestId);
        if (existingRequest.isPresent() && (!existingRequest.get().userId().equals(userId)
                || !existingRequest.get().courseId().equals(courseId)
                || !java.util.Objects.equals(existingRequest.get().skill(), target.skill()))) {
            throw conflict("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was used for a different unlock");
        }
        reconciler.reconcile(userId);
        existingRequest = unlocks.findByRequestId(requestId);
        if (existingRequest.isPresent()) {
            LearnerCourseUnlock unlock = existingRequest.get();
            if (unlock.status() == LearnerCourseUnlock.Status.ACTIVE) return CourseUnlockResult.from(unlock);
            if (unlock.status() == LearnerCourseUnlock.Status.PENDING) throw new AccessUnavailableException(null);
            throw new InsufficientPointsException();
        }
        if (!unlocks.findPending(userId).isEmpty()) throw new AccessUnavailableException(null);

        TopicResult.Course course = course(userId, courseId);
        var owned = unlocks.findActive(userId, courseId);
        boolean comboOwned = owned.stream().anyMatch(LearnerCourseUnlock::combo);
        boolean targetOwned = target == CourseUnlockTarget.COMBO ? comboOwned
                : owned.stream().anyMatch(unlock -> target.skill().equals(unlock.skill()));
        if (comboOwned || targetOwned) throw conflict("ALREADY_UNLOCKED", "This course unlock is already owned");

        Map<LearningSkill, Integer> paid = new EnumMap<>(LearningSkill.class);
        owned.stream().filter(unlock -> unlock.skill() != null)
                .forEach(unlock -> paid.put(unlock.skill(), unlock.pointCost()));
        int cost;
        try {
            cost = pricing.price(target, course.skillUnlockPoints(), course.comboUnlockPoints(),
                    course.availableSkills(), paid);
        } catch (UnlockPricingException exception) {
            throw switch (exception.code()) {
                case NOT_FOR_SALE -> conflict("NOT_FOR_SALE", "This unlock is not currently for sale");
                case SKILL_NOT_AVAILABLE -> conflict("SKILL_NOT_AVAILABLE", "This skill is not available in the course");
            };
        }

        LearnerCourseUnlock pending = intentWriter.createPending(userId, courseId, target.skill(), cost, requestId);
        try {
            UUID ledgerId = pending.pointCost() == 0 ? null : reconciler.debit(pending);
            unlocks.activate(requestId, ledgerId);
        } catch (InsufficientPointsException exception) {
            intentWriter.cancel(requestId);
            throw exception;
        }
        return unlocks.findByRequestId(requestId).map(CourseUnlockResult::from)
                .orElseThrow(() -> new IllegalStateException("Course unlock was not saved"));
    }

    private TopicResult.Course course(UUID userId, UUID courseId) {
        return refreshTopics.execute(userId).stream().map(TopicResult::course)
                .filter(java.util.Objects::nonNull).filter(item -> item.courseId().equals(courseId)).findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "COURSE_NOT_FOUND", "Course was not found"));
    }

    private static LearningRequestException conflict(String code, String detail) {
        return new LearningRequestException(409, code, detail);
    }
}

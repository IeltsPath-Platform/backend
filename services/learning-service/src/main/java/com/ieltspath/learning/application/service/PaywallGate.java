package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.port.LearningContentClient.LessonSummary;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.domain.aggregate.LessonProgress;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.service.PaywallPolicy;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Loads purchase and progress facts once per request, and asks Access for Premium only when required. */
@Component
public class PaywallGate {
    private final AccessClient access;
    private final LearnerCourseUnlockRepository unlocks;
    private final LessonProgressRepository progress;

    public PaywallGate(AccessClient access, LearnerCourseUnlockRepository unlocks,
                       LessonProgressRepository progress) {
        this.access = access;
        this.unlocks = unlocks;
        this.progress = progress;
    }

    public Request forLearner(UUID userId) {
        return new Request(userId);
    }

    public final class Request {
        private final UUID userId;
        private final Map<UUID, Ownership> ownershipByCourse = new HashMap<>();
        private boolean premiumChecked;
        private boolean premium;
        private RuntimeException premiumFailure;

        private Request(UUID userId) { this.userId = userId; }

        public boolean lessonOpen(Lesson lesson, LessonProgress lessonProgress, Set<LearningSkill> skills) {
            Ownership ownership = ownership(lesson.courseId());
            if (PaywallPolicy.isOpen(lesson.courseId(), lesson.skillPosition(), skills, ownership.skills,
                    ownership.combo, lessonProgress != null && lessonProgress.grandfathered(), false)) return true;
            return PaywallPolicy.isOpen(lesson.courseId(), lesson.skillPosition(), skills, ownership.skills,
                    ownership.combo, lessonProgress != null && lessonProgress.grandfathered(), premium());
        }

        public boolean lessonOpen(Lesson lesson, LessonProgress lessonProgress) {
            return lessonOpen(lesson, lessonProgress, lesson.skills());
        }

        public boolean lessonOpen(LessonSummary lesson, LessonProgress lessonProgress) {
            Ownership ownership = ownership(lesson.courseId());
            boolean grandfathered = lessonProgress != null && lessonProgress.grandfathered();
            if (PaywallPolicy.isOpen(lesson.courseId(), lesson.skillPosition(), lesson.skills(), ownership.skills,
                    ownership.combo, grandfathered, false)) return true;
            return PaywallPolicy.isOpen(lesson.courseId(), lesson.skillPosition(), lesson.skills(), ownership.skills,
                    ownership.combo, grandfathered, premium());
        }

        public boolean freePreview(UUID courseId, Integer skillPosition) {
            return courseId != null && skillPosition != null
                    && skillPosition <= PaywallPolicy.FREE_LESSONS_PER_SKILL;
        }

        public boolean premiumActive() { return premium(); }

        public boolean knowledgePointOpen(Topic topic, KnowledgePoint point) {
            if (topic.course() == null || point.earliestLessonPosition() == null) return true;
            return knowledgePointOpen(topic, point,
                    progress.hasGrandfatheredLessonForKnowledgePoint(userId, point.id()));
        }

        public boolean knowledgePointOpen(Topic topic, KnowledgePoint point, boolean grandfathered) {
            UUID courseId = topic.course() == null ? null : topic.course().courseId();
            Integer position = point.earliestLessonPosition();
            if (courseId == null || position == null) return true;
            LearningSkill skill = point.skill() == null ? topic.skill() : parseSkill(point.skill());
            Set<LearningSkill> pointSkills = skill == null ? Set.of() : Set.of(skill);
            Ownership ownership = ownership(courseId);
            if (PaywallPolicy.isOpen(courseId, position, pointSkills, ownership.skills, ownership.combo,
                    grandfathered, false)) return true;
            return PaywallPolicy.isOpen(courseId, position, pointSkills, ownership.skills, ownership.combo,
                    grandfathered, premium());
        }

        public void requireLessonOpen(Lesson lesson, LessonProgress lessonProgress, Set<LearningSkill> skills) {
            if (!lessonOpen(lesson, lessonProgress, skills)) {
                throw new LearningGateException("PAYWALL", java.util.List.of());
            }
        }

        public void requireKnowledgePointOpen(Topic topic, KnowledgePoint point) {
            if (!knowledgePointOpen(topic, point)) {
                throw new LearningGateException("PAYWALL", java.util.List.of());
            }
        }

        private Ownership ownership(UUID courseId) {
            if (courseId == null) return Ownership.NONE;
            return ownershipByCourse.computeIfAbsent(courseId, id -> {
                Set<LearningSkill> skills = EnumSet.noneOf(LearningSkill.class);
                boolean combo = false;
                for (LearnerCourseUnlock unlock : unlocks.findActive(userId, id)) {
                    if (unlock.status() != LearnerCourseUnlock.Status.ACTIVE) continue;
                    if (unlock.combo()) combo = true;
                    else skills.add(unlock.skill());
                }
                return new Ownership(skills, combo);
            });
        }

        private boolean premium() {
            if (!premiumChecked) {
                premiumChecked = true;
                try {
                    premium = access.premium();
                } catch (AccessUnavailableException failure) {
                    premiumFailure = failure;
                }
            }
            if (premiumFailure != null) throw premiumFailure;
            return premium;
        }
    }

    private static LearningSkill parseSkill(String skill) {
        try { return LearningSkill.valueOf(skill); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private record Ownership(Set<LearningSkill> skills, boolean combo) {
        private static final Ownership NONE = new Ownership(Set.of(), false);
    }
}

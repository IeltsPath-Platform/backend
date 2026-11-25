package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
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

/**
 * Loads purchase and progress facts once per request. Premium status is reserved for plan features and does not open
 * paid course content. A free course is open to everyone.
 */
@Component
public class PaywallGate {
    private final AccessClient access;
    private final LearnerCourseUnlockRepository unlocks;
    private final LessonProgressRepository progress;
    private final LearningContentClient content;

    public PaywallGate(AccessClient access, LearnerCourseUnlockRepository unlocks,
                       LessonProgressRepository progress, LearningContentClient content) {
        this.access = access;
        this.unlocks = unlocks;
        this.progress = progress;
        this.content = content;
    }

    public Request forLearner(UUID userId) {
        return new Request(userId);
    }

    public final class Request {
        private final UUID userId;
        private final Map<UUID, Ownership> ownershipByCourse = new HashMap<>();
        private Set<UUID> freeCourses;
        private boolean premiumChecked;
        private boolean premium;
        private RuntimeException premiumFailure;

        private Request(UUID userId) { this.userId = userId; }

        public boolean lessonOpen(Lesson lesson, LessonProgress lessonProgress, Set<LearningSkill> skills) {
            return lessonOpen(lesson.courseId(), lesson.skillPosition(), skills, grandfathered(lessonProgress));
        }

        public boolean lessonOpen(Lesson lesson, LessonProgress lessonProgress) {
            return lessonOpen(lesson, lessonProgress, lesson.skills());
        }

        public boolean lessonOpen(LessonSummary lesson, LessonProgress lessonProgress) {
            return lessonOpen(lesson.courseId(), lesson.skillPosition(), lesson.skills(), grandfathered(lessonProgress));
        }

        /**
         * The lesson rule on raw facts, for callers that hold only a catalog summary. A paid lesson whose Premium state
         * Whether a learner is Premium does not affect this paywall; a purchase, free course, free preview or prior
         * progress can open the lesson.
         */
        public boolean lessonOpen(UUID courseId, Integer skillPosition, Set<LearningSkill> skills, boolean grandfathered) {
            Ownership ownership = ownership(courseId);
            return PaywallPolicy.isOpen(courseId, skillPosition, skills, ownership.skills, ownership.combo,
                    grandfathered, false) || freeCourse(courseId);
        }

        private static boolean grandfathered(LessonProgress lessonProgress) {
            return lessonProgress != null && lessonProgress.grandfathered();
        }

        /** The first lessons of a skill that a paying course shows free; a free course has no preview, all is free. */
        public boolean freePreview(UUID courseId, Integer skillPosition) {
            return courseId != null && skillPosition != null
                    && skillPosition <= PaywallPolicy.FREE_LESSONS_PER_SKILL && !freeCourse(courseId);
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
            LearningSkill skill = parseSkill(point.skill());
            if (skill == null) skill = topic.skill();
            Set<LearningSkill> pointSkills = skill == null ? Set.of() : Set.of(skill);
            Ownership ownership = ownership(courseId);
            return PaywallPolicy.isOpen(courseId, position, pointSkills, ownership.skills, ownership.combo,
                    grandfathered, false) || freeCourse(courseId);
        }

        /**
         * Whether a paying course teaches the point in one of the first lessons of its skill, the ones it shows free: the same
         * rule as {@link #freePreview} for a lesson, read from the earliest lesson that teaches the point. Whether this learner
         * has paid does not matter, so a Premium learner still sees which points are the free ones; a free course has no preview.
         */
        public boolean knowledgePointFreePreview(Topic topic, KnowledgePoint point) {
            UUID courseId = topic.course() == null ? null : topic.course().courseId();
            return freePreview(courseId, point.earliestLessonPosition());
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

        /** Read once per request, and only once a lesson is found closed to the learner's own purchases. */
        private boolean freeCourse(UUID courseId) {
            if (courseId == null) return false;
            if (freeCourses == null) freeCourses = Set.copyOf(content.getFreeCourseIds());
            return freeCourses.contains(courseId);
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
        if (skill == null || skill.isBlank()) return null;
        try { return LearningSkill.valueOf(skill.trim()); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private record Ownership(Set<LearningSkill> skills, boolean combo) {
        private static final Ownership NONE = new Ownership(Set.of(), false);
    }
}

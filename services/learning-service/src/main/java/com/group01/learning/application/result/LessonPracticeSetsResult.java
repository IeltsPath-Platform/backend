package com.group01.learning.application.result;

import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticePassReason;
import com.group01.learning.domain.vo.PracticeStatus;

import java.util.List;
import java.util.UUID;

public record LessonPracticeSetsResult(UUID lessonId, LearningSkill skill, boolean lessonCompleted,
                                       PracticeStatus practiceStatus, PracticePassReason practicePassReason,
                                       List<Item> items) {
    public record Item(UUID packageId, String code, String title, int questionCount, String accessLevel,
                       String status, Double bestPercent, UUID lastAttemptId, boolean revealed) {}
}

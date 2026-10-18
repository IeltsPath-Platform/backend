package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.CourseMilestoneCountRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The number of practice-test milestones of a course. Content only answers a call that carries a learner's gateway
 * token, so a request reads it from Content; applying an assessment event has no token and uses the count kept when
 * the course test was assigned, which always happens through a request.
 */
@Component
public class CourseMilestones {
    private final LearningContentClient content;
    private final CourseMilestoneCountRepository counts;

    public CourseMilestones(LearningContentClient content, CourseMilestoneCountRepository counts) {
        this.content = content;
        this.counts = counts;
    }

    /** Reads the count from Content, or the kept one when the call carries no learner token. */
    public int count(UUID courseId) {
        try {
            return content.getPracticeTestMilestones(courseId).size();
        } catch (LearningRequestException noLearnerToken) {
            if (noLearnerToken.getStatus() != 401) throw noLearnerToken;
            return counts.find(courseId).orElseThrow(() -> noLearnerToken);
        }
    }

    /** Keeps the current count for the events of this course; call it from a request that may write. */
    public void keep(UUID courseId) {
        counts.save(courseId, content.getPracticeTestMilestones(courseId).size());
    }
}

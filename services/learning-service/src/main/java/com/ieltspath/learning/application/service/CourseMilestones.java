package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.CourseMilestoneCountRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The number of practice-test milestones of a course. Content only answers a call that carries a gateway token of a
 * learner, so the number is read from Content inside a request and kept; applying an assessment event has no token and
 * must not call Content, so it reads only the kept number. A course test can be assigned only through a request, which
 * keeps the number if it is not there yet, and a request that reads the milestones refreshes it when it changed.
 */
@Component
public class CourseMilestones {
    private final LearningContentClient content;
    private final CourseMilestoneCountRepository counts;

    public CourseMilestones(LearningContentClient content, CourseMilestoneCountRepository counts) {
        this.content = content;
        this.counts = counts;
    }

    /** For a request: reads the count from Content and keeps it when it is new or changed. */
    public int countForRequest(UUID courseId) {
        int count = content.getPracticeTestMilestones(courseId).size();
        counts.saveIfChanged(courseId, count);
        return count;
    }

    /** For a request that already holds the milestones it read from Content. */
    public void remember(UUID courseId, int count) {
        counts.saveIfChanged(courseId, count);
    }

    /** For a request: makes sure a count is kept, asking Content only when none is. */
    public void ensureKnown(UUID courseId) {
        if (counts.find(courseId).isEmpty()) countForRequest(courseId);
    }

    /** For an event: the kept count, never a call to Content. Without one the event fails and is tried again. */
    public int countForEvent(UUID courseId) {
        return counts.find(courseId).orElseThrow(() -> new LearningRequestException(503, "MILESTONE_COUNT_UNKNOWN",
                "The practice test milestones of this course are not known yet"));
    }
}

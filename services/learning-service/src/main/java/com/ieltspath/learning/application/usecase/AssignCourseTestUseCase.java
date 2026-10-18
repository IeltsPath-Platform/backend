package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.CourseMilestones;
import com.ieltspath.learning.application.port.LearningContentClient.TestPackage;
import com.ieltspath.learning.application.result.TestAssignmentResult;
import com.ieltspath.learning.domain.aggregate.CourseTestAssignment;
import com.ieltspath.learning.domain.repository.CourseOutcomeRepository;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import com.ieltspath.learning.domain.vo.CourseOutcome;
import com.ieltspath.learning.domain.repository.CourseTestAssignmentRepository;
import com.ieltspath.learning.domain.vo.TopicStatus;
import com.ieltspath.learning.domain.service.PackageRotation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssignCourseTestUseCase {
    private final LearnerLock lock;
    private final RefreshLearningTopicsUseCase refreshTopics;
    private final CourseProgressRepository progress;
    private final CourseTestAssignmentRepository assignments;
    private final LearningContentClient content;
    private final CourseOutcomeRepository outcomes;
    private final CourseMilestones milestones;

    public AssignCourseTestUseCase(LearnerLock lock, RefreshLearningTopicsUseCase refreshTopics,
                                   CourseProgressRepository progress, CourseTestAssignmentRepository assignments,
                                   LearningContentClient content, CourseOutcomeRepository outcomes,
                                   CourseMilestones milestones) {
        this.outcomes = outcomes;
        this.milestones = milestones;
        this.lock = lock;
        this.refreshTopics = refreshTopics;
        this.progress = progress;
        this.assignments = assignments;
        this.content = content;
    }

    @Transactional(noRollbackFor = LearningRequestException.class)
    public TestAssignmentResult execute(UUID userId, UUID courseId) {
        lock.lock(userId);
        List<com.ieltspath.learning.application.result.TopicResult> topics = refreshTopics.execute(userId);
        var courseTopics = topics.stream().filter(topic -> topic.course() != null
                && topic.course().courseId().equals(courseId)).toList();
        if (courseTopics.isEmpty()) {
            throw new LearningRequestException(404, "COURSE_NOT_FOUND", "Course was not found");
        }
        var course = courseTopics.getFirst().course();
        if (!courseTopics.getFirst().hasCourseTest()) {
            throw new LearningRequestException(409, "NO_COURSE_TEST", "This course has no final test");
        }
        // Every topic test taken opens the course test; the scores do not matter and neither do pending reviews.
        if (courseTopics.stream().anyMatch(topic -> topic.status() != TopicStatus.PASSED
                && topic.status() != TopicStatus.COMPLETED)) {
            throw new LearningRequestException(403, "COURSE_TEST_LOCKED", "Complete all course topics first");
        }
        var courseProgress = progress.findAll(userId).get(courseId);
        if (courseProgress != null && courseProgress.passedAt() != null) {
            throw new LearningRequestException(409, "COURSE_ALREADY_PASSED", "This course has already been passed");
        }
        // A retest follows the supplementary work: the reviews of the last course test must be finished first.
        if (outcomes.find(userId, courseId).filter(outcome -> !outcome.retestReady()
                && outcome.status() == CourseOutcome.Status.REMEDIATION).isPresent()) {
            throw new LearningRequestException(409, "REMEDIATION_REQUIRED",
                    "Finish the reviews from the course test before taking it again");
        }
        // The result of this test is applied by an event that cannot ask Content for the milestones itself.
        milestones.keep(courseId);
        var open = assignments.findOpen(userId, courseId);
        if (open.isPresent()) return result(open.get());
        List<TestPackage> packages = content.getCourseTestPackages(courseId);
        UUID chosenId = PackageRotation.leastRecentlyUsed(packages.stream()
                        .map(pkg -> new PackageRotation.Candidate(pkg.packageId(), pkg.code())).toList(),
                        assignments.lastConsumedAt(userId, courseId))
                .orElseThrow(() -> new LearningRequestException(409, "TEST_UNAVAILABLE",
                        "No published course test is available"));
        TestPackage chosen = packages.stream().filter(pkg -> pkg.packageId().equals(chosenId)).findFirst().orElseThrow();
        CourseTestAssignment assignment = CourseTestAssignment.assign(UUID.randomUUID(), userId, courseId,
                chosen.packageId(), chosen.packageVersionId());
        assignments.save(assignment);
        return result(assignment);
    }

    private static TestAssignmentResult result(CourseTestAssignment assignment) {
        return new TestAssignmentResult(assignment.id(), assignment.packageId(), assignment.packageVersionId());
    }
}

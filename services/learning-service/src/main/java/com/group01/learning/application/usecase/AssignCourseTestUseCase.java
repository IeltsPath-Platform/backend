package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.TestPackage;
import com.group01.learning.application.result.TestAssignmentResult;
import com.group01.learning.domain.aggregate.CourseTestAssignment;
import com.group01.learning.domain.repository.CourseProgressRepository;
import com.group01.learning.domain.repository.CourseTestAssignmentRepository;
import com.group01.learning.domain.vo.TopicStatus;
import com.group01.learning.domain.service.PackageRotation;
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

    public AssignCourseTestUseCase(LearnerLock lock, RefreshLearningTopicsUseCase refreshTopics,
                                   CourseProgressRepository progress, CourseTestAssignmentRepository assignments,
                                   LearningContentClient content) {
        this.lock = lock;
        this.refreshTopics = refreshTopics;
        this.progress = progress;
        this.assignments = assignments;
        this.content = content;
    }

    @Transactional(noRollbackFor = LearningRequestException.class)
    public TestAssignmentResult execute(UUID userId, UUID courseId) {
        lock.lock(userId);
        List<com.group01.learning.application.result.TopicResult> topics = refreshTopics.execute(userId);
        var courseTopics = topics.stream().filter(topic -> topic.course() != null
                && topic.course().courseId().equals(courseId)).toList();
        if (courseTopics.isEmpty()) {
            throw new LearningRequestException(404, "COURSE_NOT_FOUND", "Course was not found");
        }
        var course = courseTopics.getFirst().course();
        if (!courseTopics.getFirst().hasCourseTest()) {
            throw new LearningRequestException(409, "NO_COURSE_TEST", "This course has no final test");
        }
        if (courseTopics.stream().anyMatch(topic -> topic.status() != TopicStatus.PASSED)) {
            throw new LearningRequestException(403, "COURSE_TEST_LOCKED", "Complete all course topics first");
        }
        var courseProgress = progress.findAll(userId).get(courseId);
        if (courseProgress != null && courseProgress.passedAt() != null) {
            throw new LearningRequestException(409, "COURSE_ALREADY_PASSED", "This course has already been passed");
        }
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

package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.TestAssignmentResult;
import com.ieltspath.learning.domain.aggregate.PracticeTestAssignment;
import com.ieltspath.learning.domain.repository.PracticeTestAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class AssignPracticeTestUseCase {
    private final LearnerLock lock;
    private final ListPracticeTestsUseCase list;
    private final PracticeTestAssignmentRepository assignments;
    public AssignPracticeTestUseCase(LearnerLock lock,ListPracticeTestsUseCase list,PracticeTestAssignmentRepository assignments) {
        this.lock=lock;this.list=list;this.assignments=assignments;
    }
    @Transactional
    public TestAssignmentResult execute(UUID userId,UUID courseId,UUID packageId) {
        lock.lock(userId);
        var milestone=list.execute(userId,courseId).stream().filter(m -> m.packageId().equals(packageId)).findFirst()
                .orElseThrow(() -> new LearningRequestException(404,"PRACTICE_TEST_NOT_FOUND","Practice test is not a milestone of this course"));
        if ("LOCKED".equals(milestone.status())) throw new LearningRequestException(403,"PRACTICE_TEST_LOCKED","Pass the milestone topic first");
        var existing=assignments.find(userId,packageId);
        if (existing.isPresent()) {
            var a=existing.get();
            if (!a.courseId().equals(courseId)) throw new LearningRequestException(409,"PRACTICE_TEST_ALREADY_ASSIGNED",
                    "This practice test was assigned in another course");
            // A retake may use the current version, while the stored first result and its version stay frozen.
            return new TestAssignmentResult(a.id(),packageId,a.isConsumed()?milestone.packageVersionId():a.packageVersionId());
        }
        var a=PracticeTestAssignment.assign(userId,courseId,packageId,milestone.packageVersionId(),Instant.now());
        assignments.save(a);
        return new TestAssignmentResult(a.id(),packageId,a.packageVersionId());
    }
}

package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ReleasePlacementAssignmentUseCase {
    private final PlacementAssignmentRepository assignments;
    private final LearnerLock lock;
    private final Clock clock;

    @Autowired
    public ReleasePlacementAssignmentUseCase(PlacementAssignmentRepository assignments, LearnerLock lock) {
        this(assignments, lock, Clock.systemUTC());
    }
    ReleasePlacementAssignmentUseCase(PlacementAssignmentRepository assignments, LearnerLock lock, Clock clock) {
        this.assignments = assignments; this.lock = lock; this.clock = clock;
    }

    @Transactional(noRollbackFor = LearningRequestException.class)
    public void execute(UUID userId, UUID assignmentId) {
        lock.lock(userId);
        var assignment = assignments.findOwnedOpen(userId, assignmentId).orElseThrow(() ->
                new LearningRequestException(404, "PLACEMENT_ASSIGNMENT_NOT_FOUND", "Không tìm thấy lượt giao bài kiểm tra đầu vào."));
        assignment.release(clock.instant());
        assignments.save(assignment);
    }
}

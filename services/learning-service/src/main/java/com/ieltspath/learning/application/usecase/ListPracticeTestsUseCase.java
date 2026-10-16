package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.PracticeTestMilestoneResult;
import com.ieltspath.learning.domain.aggregate.PracticeTestAssignment;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.PracticeTestAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListPracticeTestsUseCase {
    private final LearnerLock lock;
    private final RefreshLearningTopicsUseCase refresh;
    private final LearnerCurriculumRepository curricula;
    private final LearningContentClient content;
    private final PracticeTestAssignmentRepository assignments;
    public ListPracticeTestsUseCase(LearnerLock lock,RefreshLearningTopicsUseCase refresh,
            LearnerCurriculumRepository curricula,LearningContentClient content,PracticeTestAssignmentRepository assignments) {
        this.lock=lock;this.refresh=refresh;this.curricula=curricula;this.content=content;this.assignments=assignments;
    }
    @Transactional
    public List<PracticeTestMilestoneResult> execute(UUID userId,UUID courseId) {
        lock.lock(userId);
        refresh.execute(userId);
        var topics=curricula.find(userId).topics().stream()
                .filter(t -> courseId.equals(t.courseId()) && t.sequenceOrder()!=null)
                .sorted(Comparator.comparingInt(TopicProgress::sequenceOrder)).toList();
        if (topics.isEmpty()) throw new LearningRequestException(404,"COURSE_NOT_FOUND","Active course not found");
        var milestones=content.getPracticeTestMilestones(courseId);
        var assigned=assignments.findByCourse(userId,courseId).stream()
                .collect(Collectors.toMap(PracticeTestAssignment::packageId,Function.identity()));
        return milestones.stream().sorted(Comparator.comparingInt(LearningContentClient.PracticeTestMilestone::afterTopicSequence)
                .thenComparing(LearningContentClient.PracticeTestMilestone::code)).map(m -> {
            var a=assigned.get(m.packageId());
            boolean done=a!=null && a.isConsumed();
            // Positions are local to this course, whereas topic_progress.sequence_order is global.
            boolean available=m.afterTopicSequence()>0 && m.afterTopicSequence()<=topics.size()
                    && topics.get(m.afterTopicSequence()-1).passedAt()!=null;
            return new PracticeTestMilestoneResult(m.packageId(),m.packageVersionId(),m.code(),m.title(),
                    m.afterTopicSequence(),done?"DONE":available?"AVAILABLE":"LOCKED",a==null?null:a.percent());
        }).toList();
    }
}

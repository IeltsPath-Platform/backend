package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.PracticeTestMilestoneRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetPracticeTestMilestonesUseCase {
    private final CourseRepository courses;
    private final PracticeTestMilestoneRepository milestones;
    public GetPracticeTestMilestonesUseCase(CourseRepository courses, PracticeTestMilestoneRepository milestones) {
        this.courses = courses; this.milestones = milestones;
    }
    public List<PracticeTestMilestone> execute(UUID courseId, boolean publishedOnly) {
        courses.findById(courseId).filter(c -> !publishedOnly || c.getStatus()==ContentStatus.ACTIVE)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        return milestones.findByCourse(courseId, publishedOnly);
    }
}

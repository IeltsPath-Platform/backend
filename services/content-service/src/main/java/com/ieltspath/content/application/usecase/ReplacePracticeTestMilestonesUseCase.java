package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.PracticeTestMilestoneRepository;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ReplacePracticeTestMilestonesUseCase {
    private final CourseRepository courses;
    private final PracticeTestMilestoneRepository milestones;
    public ReplacePracticeTestMilestonesUseCase(CourseRepository courses, PracticeTestMilestoneRepository milestones) {
        this.courses = courses; this.milestones = milestones;
    }
    @Transactional
    public List<PracticeTestMilestone> execute(UUID courseId, List<PracticeTestMilestone> entries) {
        milestones.lockCourse(courseId);
        courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        if (entries==null || entries.size()>100) throw invalid();
        int topics = milestones.topicCount(courseId);
        Set<UUID> ids = new HashSet<>();
        for (var entry : entries) {
            if (entry==null || entry.packageId()==null || !ids.add(entry.packageId())
                    || entry.afterTopicSequence()<1 || entry.afterTopicSequence()>topics) throw invalid();
        }
        if (!milestones.publishedPracticeTests(ids).equals(ids)) throw invalid();
        if (!milestones.packagesInOtherCourses(courseId, ids).isEmpty()) throw invalid();
        milestones.replace(courseId, entries);
        return milestones.findByCourse(courseId, false);
    }
    private static InvalidContentStateException invalid() {
        return new InvalidContentStateException("Milestones require unique published PRACTICE_TEST packages and a reachable topic position (maximum 100)");
    }
}

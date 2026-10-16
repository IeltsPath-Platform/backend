package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PracticeTestMilestoneRepository {
    List<PracticeTestMilestone> findByCourse(UUID courseId, boolean publishedOnly);
    void lockCourse(UUID courseId);
    int topicCount(UUID courseId);
    Set<UUID> publishedPracticeTests(Collection<UUID> packageIds);
    Set<UUID> packagesInOtherCourses(UUID courseId, Collection<UUID> packageIds);
    void replace(UUID courseId, List<PracticeTestMilestone> milestones);
}

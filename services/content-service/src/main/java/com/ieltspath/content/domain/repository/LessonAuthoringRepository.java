package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.Lesson;
import com.ieltspath.content.domain.vo.LessonAuthoringSnapshot;
import com.ieltspath.content.domain.vo.Skill;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

public interface LessonAuthoringRepository {
    boolean topicExists(UUID topicId);
    Optional<Skill> courseTopicSkillForLesson(UUID lessonId);
    Map<UUID, Skill> knowledgePointSkills(List<UUID> knowledgePointIds);
    Lesson create(UUID topicId, String code, String title, String summary, String summaryMarkdown,
                                 Integer estimatedMinutes, Integer sortOrder);
    List<LessonAuthoringSnapshot> findByTopic(UUID topicId);
    Optional<LessonAuthoringSnapshot> findSnapshotById(UUID lessonId);
    Optional<Lesson> lockAndFindById(UUID lessonId);
    Lesson save(Lesson lesson);
    void reorderLessons(UUID topicId, List<UUID> lessonIds);
    List<String> publicationViolations(UUID lessonId);

    /**
     * Those of the given question versions that already belong to an exercise block other than {@code exceptBlockId}
     * (none excepted when it is null), or to a package section, in the order given.
     */
    List<UUID> questionVersionsOwnedElsewhere(UUID exceptBlockId, List<UUID> questionVersionIds);
}

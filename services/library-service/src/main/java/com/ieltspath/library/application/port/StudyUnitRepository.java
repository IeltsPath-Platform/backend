package com.ieltspath.library.application.port;

import com.ieltspath.library.domain.study.StudyWordSnapshot;
import java.time.Instant;
import java.util.*;

public interface StudyUnitRepository {
    record Unit(UUID id, UUID topicId, int sortOrder, String kind, Integer publishedRevision, Integer draftRevision,
                Instant archivedAt, Instant updatedAt, int revision, String title, String description, String introductionVi,
                List<String> exerciseTypes, Integer gradedStepCount) {}
    record Draft(String title, String description, String kind, String introductionVi, List<UUID> senseIds) {}
    boolean activeTopic(UUID topicId);
    boolean sensesExist(List<UUID> senseIds);
    void lockTopic(UUID topicId);
    Optional<Unit> find(UUID unitId, boolean draft, boolean lock);
    List<Unit> list(UUID topicId, boolean author);
    List<StudyWordSnapshot> words(UUID unitId, int revision, boolean activeOnly);
    Map<UUID,List<StudyWordSnapshot>> words(List<Unit> units, boolean activeOnly);
    List<StudyWordSnapshot> distractors(UUID topicId);
    int version(UUID topicId);
    void bumpVersion(UUID topicId, Instant now);
    UUID create(UUID topicId, UUID author, Draft draft, Instant now);
    void update(Unit unit, UUID author, Draft draft, Instant now);
    void publish(Unit unit, List<String> types, Integer graded, Instant now);
    void archive(UUID unitId, Instant now);
    void reorder(List<UUID> ids, Instant now);
}

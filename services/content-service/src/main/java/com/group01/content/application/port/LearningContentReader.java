package com.group01.content.application.port;

import com.group01.content.application.result.LessonContentResult;
import com.group01.content.application.result.LessonSummaryResult;
import com.group01.content.application.result.PackageVersionContentResult;
import com.group01.content.application.result.PracticeSetResult;
import com.group01.content.application.result.TopicSequenceResult;
import com.group01.content.application.result.TopicTestPackageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read projections of the published curriculum for the learning flow. Every method runs a fixed number of queries,
 * whatever the size of the result.
 *
 * <p>A practice set is <em>eligible</em> for a knowledge point when it is a published {@code PRACTICE_SET} whose
 * current version has at least {@code minQuestions} questions, at least one of them measuring the knowledge point,
 * and none of them used by a lesson or by any {@code TOPIC_TEST} package. {@link #topicSequence} and
 * {@link #searchPracticeSets} share this rule.
 */
public interface LearningContentReader {

    /** Active topics with a published lesson and a published final test, in learning order. */
    List<TopicSequenceResult> topicSequence(int minPracticeQuestions);

    boolean activeTopicExists(UUID topicId);

    List<LessonSummaryResult> publishedLessons(UUID topicId);

    Optional<LessonContentResult> publishedLesson(UUID lessonId);

    List<TopicTestPackageResult> publishedTestPackages(UUID topicId);

    /** Eligible practice sets, most matching questions first, then by package id. */
    List<PracticeSetResult> searchPracticeSets(UUID knowledgePointId, Collection<UUID> excludePackageIds,
                                               int minQuestions, int limit);

    Optional<PackageVersionContentResult> publishedPackageVersion(UUID packageVersionId);

    /** The given question versions that a lesson, a practice set or a final test uses. */
    Set<UUID> questionVersionsReservedForLearning(Collection<UUID> questionVersionIds);
}

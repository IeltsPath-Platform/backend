package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.application.result.LessonPracticeSetResult;
import com.ieltspath.content.application.result.LessonPracticeSetsResult;
import com.ieltspath.content.application.result.LessonSummaryResult;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.PracticeSetResult;
import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.application.result.TopicTestPackageResult;
import com.ieltspath.content.application.result.PackageQuestionSpec;
import com.ieltspath.content.application.result.KnowledgeTreeNodeResult;
import com.ieltspath.content.domain.vo.QuestionUsageConflict;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.Skill;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read projections of the curriculum for the learning flow and author preview. Every method runs a fixed number of
 * queries, whatever the size of the result; {@link #packageVersionContent(UUID)} may include a draft version.
 *
 * <p>A practice set is <em>eligible</em> for a knowledge point when it is a published {@code PRACTICE_SET} whose
 * current version has at least {@code minQuestions} questions, at least one of them measuring the knowledge point,
 * and none of them used by a lesson or by any {@code TOPIC_TEST}, {@code COURSE_TEST}, {@code MOCK_TEST} or {@code PLACEMENT_TEST}
 * package. {@link #topicSequence} and
 * {@link #searchPracticeSets} share this rule.
 */
public interface LearningContentReader {

    com.ieltspath.content.application.result.ProgressMetadataResult progressMetadata(Collection<UUID> courseIds);

    /** Active topics in active courses, with a skill and published lesson, by skill, course band, topic order and id. */
    List<TopicSequenceResult> topicSequence(int minPracticeQuestions);

    boolean activeTopicExists(UUID topicId);

    List<LessonSummaryResult> publishedLessons(UUID topicId);

    Optional<LessonContentResult> publishedLesson(UUID lessonId);

    boolean publishedLessonExists(UUID lessonId);

    boolean lessonExists(UUID lessonId);

    boolean topicExists(UUID topicId);

    boolean courseExists(UUID courseId);

    /** Published practice sets of the lesson, by code. */
    List<LessonPracticeSetResult> lessonPracticeSets(UUID lessonId);

    List<LessonPracticeSetResult> lessonPracticeSets(UUID lessonId, Optional<Skill> skill);

    /** Every published lesson of the topic in order, each with its published practice sets by code. */
    List<LessonPracticeSetsResult> topicPracticeSets(UUID topicId);

    /**
     * For each knowledge point, how many eligible practice sets are not in {@code excludePackageIds}. Unknown
     * knowledge points count zero.
     */
    Map<UUID, Integer> countEligiblePracticeSets(Collection<UUID> knowledgePointIds,
                                                 Collection<UUID> excludePackageIds, int minQuestions);

    /** Whether a question of the package version has a skill outside the lesson's taught skills. */
    boolean packageVersionLeavesLessonSkills(UUID packageVersionId, UUID lessonId);

    /**
     * Questions of this version used by any lesson or a published version of another {@code PRACTICE_SET},
     * {@code TOPIC_TEST}, {@code COURSE_TEST}, {@code MOCK_TEST} or {@code PLACEMENT_TEST} package, compared by question id across all
     * question versions. Versions of the same package are not conflicts. Loaded in one query.
     */
    List<QuestionUsageConflict> questionsUsedElsewhere(UUID packageVersionId);

    /** Lock this version's question rows in id order until transaction commit, before checking ownership. */
    void lockQuestionsForPublishing(UUID packageVersionId);

    /** Distinct question ids in this version whose purpose differs from the required package purpose. */
    List<UUID> questionsWithWrongPurpose(UUID packageVersionId, QuestionPurpose requiredPurpose);

    List<TopicTestPackageResult> publishedTestPackages(UUID topicId);

    /** Published course final tests with their current published version, ordered by package id. */
    List<TopicTestPackageResult> courseTestPackages(UUID courseId);

    /** Published placement tests with their current published version, ordered by package id. */
    List<TopicTestPackageResult> placementTestPackages();

    /** All question versions of a package version, including drafts, loaded in one query for publish validation. */
    List<PackageQuestionSpec> packageQuestionSpecs(UUID packageVersionId);

    /**
     * Eligible practice sets: those of {@code preferredLessonId} first when it is given, then most matching
     * questions, then by package id.
     */
    List<PracticeSetResult> searchPracticeSets(UUID knowledgePointId, Collection<UUID> excludePackageIds,
                                               int minQuestions, int limit, UUID preferredLessonId);

    Optional<PackageVersionContentResult> publishedPackageVersion(UUID packageVersionId);

    /** Package version content for author preview, including draft versions. */
    Optional<PackageVersionContentResult> packageVersionContent(UUID packageVersionId);

    default List<String> practiceTestPublishIssues(UUID packageVersionId) { return List.of(); }

    /** The given question versions that a lesson, practice set, final test, mock test or placement test uses. */
    Set<UUID> questionVersionsReservedForLearning(Collection<UUID> questionVersionIds);

    List<KnowledgeTreeNodeResult> knowledgeTreeNodes(UUID courseId, Skill skill, boolean includeInactive);

    /** Active knowledge points mapped to their active, direct prerequisites; unknown and inactive ids are omitted. */
    Map<UUID, List<UUID>> activeKnowledgePrerequisites(Collection<UUID> ids);

    /** Question versions with a classified difficulty; unknown and unclassified ids are omitted. */
    Map<UUID, QuestionDifficulty> questionDifficulties(Collection<UUID> questionVersionIds);

    /** Names of knowledge points and the start of question stems, for the staff work queue; unknown ids are omitted. */
    com.ieltspath.content.application.result.WorkItemSubjectsResult workItemSubjects(
            Collection<UUID> knowledgePointIds, Collection<UUID> questionVersionIds);
}

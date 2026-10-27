package com.ieltspath.content.application.service;

import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.application.command.CreateTopicCommand;
import com.ieltspath.content.application.command.UpdateCourseCommand;
import com.ieltspath.content.application.command.UpdateTopicCommand;
import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.application.result.LessonAuthoringResult;
import com.ieltspath.content.application.result.TopicResult;
import com.ieltspath.content.application.usecase.CreateContentPackageUseCase;
import com.ieltspath.content.application.usecase.CreateCourseUseCase;
import com.ieltspath.content.application.usecase.CreateDraftPackageVersionUseCase;
import com.ieltspath.content.application.usecase.CreateTopicUseCase;
import com.ieltspath.content.application.usecase.LessonAuthoringUseCase;
import com.ieltspath.content.application.usecase.UpdateCourseUseCase;
import com.ieltspath.content.application.usecase.UpdateTopicUseCase;
import com.ieltspath.content.domain.exception.InvalidPackageLessonException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.PracticeTestMilestoneRepository;
import com.ieltspath.content.domain.vo.BandRange;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.BlockPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.FinalTestPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.ResourcePlan;
import com.ieltspath.content.domain.vo.LessonResourceType;
import com.ieltspath.content.application.command.CreateLessonResourceCommand;
import com.ieltspath.content.application.usecase.CreateLessonResourceUseCase;
import com.ieltspath.content.domain.vo.CourseDraftStructure.TopicPlan;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns a course structure into real content, through the same use cases the authoring screens use: the course and
 * topics (inactive), the lessons (drafts) with blocks and knowledge points, the chosen practice sets, the empty final
 * tests and the milestones of existing practice tests. It runs inside the caller's transaction, so any failure rolls all
 * of it back. Course drafts and workbook imports both end here.
 */
@Service
public class CourseMaterializer {

    private final CreateCourseUseCase createCourse;
    private final UpdateCourseUseCase updateCourse;
    private final CreateTopicUseCase createTopic;
    private final UpdateTopicUseCase updateTopic;
    private final LessonAuthoringUseCase lessons;
    private final ContentPackageRepository packages;
    private final CreateContentPackageUseCase createPackage;
    private final CreateDraftPackageVersionUseCase createDraftVersion;
    private final PracticeTestMilestoneRepository milestones;
    private final CreateLessonResourceUseCase lessonResources;

    public CourseMaterializer(CreateCourseUseCase createCourse, UpdateCourseUseCase updateCourse,
                              CreateTopicUseCase createTopic, UpdateTopicUseCase updateTopic,
                              LessonAuthoringUseCase lessons, ContentPackageRepository packages,
                              CreateContentPackageUseCase createPackage,
                              CreateDraftPackageVersionUseCase createDraftVersion,
                              PracticeTestMilestoneRepository milestones, CreateLessonResourceUseCase lessonResources) {
        this.createCourse = createCourse;
        this.updateCourse = updateCourse;
        this.createTopic = createTopic;
        this.updateTopic = updateTopic;
        this.lessons = lessons;
        this.packages = packages;
        this.createPackage = createPackage;
        this.createDraftVersion = createDraftVersion;
        this.milestones = milestones;
        this.lessonResources = lessonResources;
    }

    /** What was created. The maps give the ids by the codes of the structure. */
    public record Created(UUID courseId, List<UUID> topicIds, List<UUID> lessonIds, List<UUID> finalTestPackageIds,
                          Map<String, UUID> topicIdsByCode, Map<String, UUID> lessonIdsByCode) {
    }

    /**
     * @param questionVersionsByRef the question version each {@code questionRefs} entry of an exercise block stands for
     */
    @Transactional
    public Created create(CourseDraftStructure plan, Map<String, UUID> questionVersionsByRef) {
        BigDecimal band = plan.course().bandLevel();
        CourseResult course = createCourse.execute(new CreateCourseCommand(plan.course().code(),
                plan.course().name(), band));
        updateCourse.execute(new UpdateCourseCommand(course.id(), plan.course().name(), band, ContentStatus.INACTIVE));

        List<UUID> topicIds = new ArrayList<>();
        List<UUID> lessonIds = new ArrayList<>();
        List<UUID> finalTests = new ArrayList<>();
        Map<String, UUID> topicsByCode = new LinkedHashMap<>();
        Map<String, UUID> lessonsByCode = new LinkedHashMap<>();
        BandRange topicBand = BandRange.of(band, band);
        for (TopicPlan topicPlan : plan.topicsInOrder()) {
            TopicResult topic = createTopic.execute(new CreateTopicCommand(null, topicPlan.code(), topicPlan.name(),
                    topicPlan.sortOrder(), topicBand, topicPlan.skill(), course.id()));
            updateTopic.execute(new UpdateTopicCommand(topic.id(), null, topicPlan.name(), topicPlan.sortOrder(),
                    ContentStatus.INACTIVE, topicBand, topicPlan.skill(), course.id()));
            topicIds.add(topic.id());
            topicsByCode.put(topicPlan.code(), topic.id());
            addLessons(topicPlan, topic.id(), questionVersionsByRef, lessonIds, lessonsByCode);
            if (topicPlan.topicTest() != null) {
                finalTests.add(createFinalTest(PackageType.TOPIC_TEST, topicPlan.topicTest(), topic.id(), null));
            }
        }
        if (plan.courseTest() != null) {
            finalTests.add(createFinalTest(PackageType.COURSE_TEST, plan.courseTest(), null, course.id()));
        }
        if (!plan.practiceTestMilestones().isEmpty()) {
            // The milestone use case only accepts positions of topics that are already live. The new topics are not,
            // so the positions were checked against the plan's own topics and the tests by the analysis instead.
            milestones.lockCourse(course.id());
            milestones.replace(course.id(), plan.practiceTestMilestones().stream()
                    .map(milestone -> new PracticeTestMilestone(milestone.packageId(), null, null, null,
                            milestone.afterTopicSequence())).toList());
        }
        return new Created(course.id(), topicIds, lessonIds, finalTests, topicsByCode, lessonsByCode);
    }

    private void addLessons(TopicPlan topic, UUID topicId, Map<String, UUID> questionVersionsByRef,
                            List<UUID> lessonIds, Map<String, UUID> lessonsByCode) {
        int position = 1;
        for (LessonPlan lessonPlan : topic.lessons()) {
            LessonAuthoringResult lesson = lessons.create(topicId, lessonPlan.code(), lessonPlan.title(),
                    lessonPlan.summary(), lessonPlan.summaryMarkdown(), lessonPlan.estimatedMinutes(), position++);
            lessonIds.add(lesson.id());
            lessonsByCode.put(lessonPlan.code(), lesson.id());
            if (!lessonPlan.knowledgePointIds().isEmpty()) {
                lessons.knowledgePoints(lesson.id(), lessonPlan.knowledgePointIds());
            }
            for (BlockPlan block : lessonPlan.blocks()) {
                addBlock(lesson.id(), block, questionVersionsByRef);
            }
            addResources(lesson.id(), lessonPlan);
            for (UUID practiceSet : lessonPlan.practiceSetPackageIds()) {
                if (!packages.attachPracticeSetToLesson(practiceSet, lesson.id())) {
                    throw new InvalidPackageLessonException("Package " + practiceSet
                            + " is not a practice set that is free to join a lesson");
                }
            }
        }
    }

    /** Slides and documents are numbered per type, in the order the author listed them. */
    private void addResources(UUID lessonId, LessonPlan lessonPlan) {
        Map<LessonResourceType, Integer> next = new java.util.EnumMap<>(LessonResourceType.class);
        for (ResourcePlan resource : lessonPlan.resources()) {
            int order = next.merge(resource.type(), 1, Integer::sum) - 1;
            lessonResources.execute(new CreateLessonResourceCommand(lessonId, resource.type(), resource.title().trim(),
                    resource.url().trim(), order));
        }
    }

    private void addBlock(UUID lessonId, BlockPlan block, Map<String, UUID> questionVersionsByRef) {
        if (block.type() == BlockType.TEXT) {
            lessons.addBlock(lessonId, BlockType.TEXT, null, block.text() == null ? "" : block.text(), null, null,
                    null, block.knowledgePointIds());
        } else {
            List<UUID> versions = block.questionRefs().stream().map(questionVersionsByRef::get).toList();
            lessons.addBlock(lessonId, BlockType.EXERCISE, null, null, null, null, versions, null);
        }
    }

    private UUID createFinalTest(PackageType type, FinalTestPlan test, UUID topicId, UUID courseId) {
        var created = createPackage.executeAsAuthor(new CreateContentPackageCommand(test.code(), test.title(), type,
                null, null, null, topicId, courseId));
        createDraftVersion.execute(created.id(), null);
        return created.id();
    }
}

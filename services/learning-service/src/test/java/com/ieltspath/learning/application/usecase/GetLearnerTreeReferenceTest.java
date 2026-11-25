package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Course;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.application.service.PaywallGate;
import com.ieltspath.learning.application.usecase.GetLearnerTreeUseCase.ReferencePoint;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What a course builds on in a lower course: those points come back as references, level by level, so the chart can show the
 * order of learning back to the start without the course teaching them again.
 */
class GetLearnerTreeReferenceTest {
    private final UUID user = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID lowerCourseId = UUID.randomUUID();
    private final UUID writingTopic = UUID.randomUUID();
    private final UUID foundationOne = UUID.randomUUID();
    private final UUID foundationTwo = UUID.randomUUID();
    private final UUID compound = UUID.randomUUID();
    private final UUID linking = UUID.randomUUID();
    private final UUID sentence = UUID.randomUUID();
    private final UUID unrelated = UUID.randomUUID();

    private final LearningContentClient content = mock(LearningContentClient.class);
    private GetLearnerTreeUseCase tree;

    @BeforeEach
    void setUp() {
        LearnerCurriculumRepository curricula = mock(LearnerCurriculumRepository.class, RETURNS_DEEP_STUBS);
        TopicProgress taking = mock(TopicProgress.class);
        when(taking.topicId()).thenReturn(writingTopic);
        when(curricula.find(user).topics()).thenReturn(List.of(taking));

        PaywallGate paywall = mock(PaywallGate.class);
        PaywallGate.Request request = mock(PaywallGate.Request.class);
        when(paywall.forLearner(user)).thenReturn(request);
        when(request.knowledgePointOpen(any(), any(), anyBoolean())).thenReturn(true);

        when(content.practiceSetAvailability(anyList(), anyList(), anyInt())).thenReturn(Map.of());
        when(content.prerequisites(anyList())).thenReturn(Map.of());
        tree = new GetLearnerTreeUseCase(curricula, mock(MasteryProfileRepository.class), mock(ReviewItemRepository.class),
                mock(PracticeAttemptRepository.class), content, paywall, mock(LessonProgressRepository.class), 2);
    }

    private Course course(UUID id, String name, double band) {
        return new Course(id, name.toUpperCase().replace(' ', '_'), name, BigDecimal.valueOf(band), false, true, null, null);
    }

    private Topic topic(UUID id, String name, int sortOrder, LearningSkill skill, Course course, KnowledgePoint... points) {
        return new Topic(id, name, name, sortOrder, null, List.of(points), skill, false, course, null, true, 0);
    }

    private KnowledgePoint point(UUID id, String code, String skill) {
        return new KnowledgePoint(id, code, code, "PROCEDURE", skill, null, true, "A2", null);
    }

    /** The course holds {@code compound}; the lower course teaches linking words, then sentence patterns before them. */
    private void courses() {
        Course current = course(courseId, "IELTS 5.0", 5.0);
        Course lower = course(lowerCourseId, "Foundation 4.0", 4.0);
        when(content.getTopicSequence(true)).thenReturn(List.of(
                topic(foundationOne, "Sentence patterns", 100, LearningSkill.WRITING, lower, point(sentence, "G40_SENTENCE", "ALL")),
                topic(foundationTwo, "Linking", 110, LearningSkill.WRITING, lower,
                        point(linking, "G40_LINKING", "ALL"), point(unrelated, "G40_UNRELATED", "ALL")),
                topic(writingTopic, "Compound", 200, LearningSkill.WRITING, current, point(compound, "W50_COMPOUND", "WRITING"))));
    }

    private List<ReferencePoint> references() {
        return tree.execute(user, courseId).referencePoints();
    }

    @Test
    void returnsWhatTheCourseBuildsOnInALowerCourseAndWhatThatBuildsOnInTurn() {
        courses();
        when(content.prerequisites(List.of(compound))).thenReturn(Map.of(compound, List.of(linking)));
        when(content.prerequisites(List.of(linking))).thenReturn(Map.of(linking, List.of(sentence)));
        when(content.prerequisites(List.of(sentence))).thenReturn(Map.of());

        List<ReferencePoint> found = references();

        assertThat(found).extracting(ReferencePoint::knowledgePointId).containsExactly(linking, sentence);
        ReferencePoint first = found.get(0);
        assertThat(first.code()).isEqualTo("G40_LINKING");
        assertThat(first.skill()).isEqualTo("ALL");
        assertThat(first.cefrLevel()).isEqualTo("A2");
        assertThat(first.prerequisiteIds()).containsExactly(sentence);
        assertThat(first.course().name()).isEqualTo("Foundation 4.0");
        assertThat(first.course().bandLevel()).isEqualByComparingTo("4.0");
        assertThat(found.get(1).prerequisiteIds()).isEmpty();
    }

    @Test
    void numbersTheChangOfAReferenceInsideItsOwnCourse() {
        courses();
        when(content.prerequisites(List.of(compound))).thenReturn(Map.of(compound, List.of(linking, sentence)));

        List<ReferencePoint> found = references();

        // Both chặng are Writing chặng of the lower course: the first is 1, the second is 2, whatever the course above has.
        assertThat(found).filteredOn(point -> point.knowledgePointId().equals(sentence)).singleElement()
                .satisfies(point -> assertThat(point.topic().position()).isEqualTo(1));
        assertThat(found).filteredOn(point -> point.knowledgePointId().equals(linking)).singleElement()
                .satisfies(point -> assertThat(point.topic().position()).isEqualTo(2));
    }

    @Test
    void leavesOutAPointNothingBuildsOnAndAPointNoCourseOwns() {
        courses();
        UUID gone = UUID.randomUUID();
        when(content.prerequisites(List.of(compound))).thenReturn(Map.of(compound, List.of(linking, gone)));

        assertThat(references()).extracting(ReferencePoint::knowledgePointId).containsExactly(linking);
    }

    @Test
    void asksContentNothingMoreWhenEverythingTheCourseBuildsOnIsInTheCourse() {
        courses();
        when(content.prerequisites(List.of(compound))).thenReturn(Map.of(compound, List.of(compound)));

        assertThat(references()).isEmpty();
        verify(content, times(1)).prerequisites(anyList());
    }

    @Test
    void stopsAfterAFewLevelsHoweverLongTheChain() {
        Course current = course(courseId, "IELTS 5.0", 5.0);
        Course lower = course(lowerCourseId, "Foundation 4.0", 4.0);
        UUID l1 = UUID.randomUUID();
        UUID l2 = UUID.randomUUID();
        UUID l3 = UUID.randomUUID();
        UUID l4 = UUID.randomUUID();
        when(content.getTopicSequence(true)).thenReturn(List.of(
                topic(foundationOne, "Chain", 100, LearningSkill.WRITING, lower,
                        point(l1, "L1", "ALL"), point(l2, "L2", "ALL"), point(l3, "L3", "ALL"), point(l4, "L4", "ALL")),
                topic(writingTopic, "Compound", 200, LearningSkill.WRITING, current, point(compound, "W50_COMPOUND", "WRITING"))));
        when(content.prerequisites(List.of(compound))).thenReturn(Map.of(compound, List.of(l1)));
        when(content.prerequisites(List.of(l1))).thenReturn(Map.of(l1, List.of(l2)));
        when(content.prerequisites(List.of(l2))).thenReturn(Map.of(l2, List.of(l3)));
        when(content.prerequisites(List.of(l3))).thenReturn(Map.of(l3, List.of(l4)));

        assertThat(references()).extracting(ReferencePoint::knowledgePointId).containsExactly(l1, l2, l3);
        verify(content, times(1 + GetLearnerTreeUseCase.REFERENCE_DEPTH)).prerequisites(anyList());
    }

    @Test
    void theCourseKeepsItsOwnPointsAsTheyWere() {
        courses();
        when(content.prerequisites(List.of(compound))).thenReturn(Map.of(compound, List.of(linking)));

        var result = tree.execute(user, courseId);

        assertThat(result.knowledgePoints()).extracting(GetLearnerTreeUseCase.Node::knowledgePointId).containsExactly(compound);
        assertThat(result.knowledgePoints().get(0).prerequisiteIds()).containsExactly(linking);
    }
}

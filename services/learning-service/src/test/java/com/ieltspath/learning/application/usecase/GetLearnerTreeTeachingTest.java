package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Course;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.LessonSummary;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.application.service.PaywallGate;
import com.ieltspath.learning.application.usecase.GetLearnerTreeUseCase.Node;
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
import static org.mockito.Mockito.when;

/** Where each point of the learner's tree is taught: the chặng that owns it and the lessons that teach it. */
class GetLearnerTreeTeachingTest {
    private final UUID user = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID readingOne = UUID.randomUUID();
    private final UUID writing = UUID.randomUUID();
    private final UUID readingTwo = UUID.randomUUID();
    private final UUID scanning = UUID.randomUUID();
    private final UUID matching = UUID.randomUUID();
    private final UUID opinion = UUID.randomUUID();

    private final LearningContentClient content = mock(LearningContentClient.class);
    private GetLearnerTreeUseCase tree;

    @BeforeEach
    void setUp() {
        LearnerCurriculumRepository curricula = mock(LearnerCurriculumRepository.class, RETURNS_DEEP_STUBS);
        TopicProgress taking = mock(TopicProgress.class);
        when(taking.topicId()).thenReturn(readingOne);
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

    private Topic topic(UUID id, String name, int sortOrder, LearningSkill skill, KnowledgePoint... points) {
        Course course = new Course(courseId, "IELTS_5_5", "IELTS 5.5", BigDecimal.valueOf(5.5), false, true, null, null);
        return new Topic(id, name, name, sortOrder, null, List.of(points), skill, false, course, null, true, 0);
    }

    private KnowledgePoint point(UUID id, String code, String skill) {
        return new KnowledgePoint(id, code, code, "PROCEDURE", skill, null, true, null, null);
    }

    private LessonSummary lesson(UUID topicId, String title, int sortOrder, UUID... points) {
        return new LessonSummary(UUID.randomUUID(), topicId, title, title, null, sortOrder, List.of(points), List.of());
    }

    private Node node(UUID id) {
        return tree.execute(user, courseId).knowledgePoints().stream()
                .filter(row -> row.knowledgePointId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void namesTheChangThatOwnsAPointAndNumbersItInsideItsSkill() {
        when(content.getTopicSequence(true)).thenReturn(List.of(
                topic(readingOne, "Demo Reading", 900, LearningSkill.READING, point(matching, "PR_MATCHING_INFORMATION", "READING")),
                topic(writing, "Writing basics", 905, LearningSkill.WRITING, point(opinion, "TR_W_OPINION", "WRITING")),
                topic(readingTwo, "Trees: Reading", 955, LearningSkill.READING, point(scanning, "TR_R_SCANNING", "READING"))));

        assertThat(node(matching).topic()).isEqualTo(new GetLearnerTreeUseCase.TeachingTopic(readingOne, "Demo Reading", 1));
        assertThat(node(scanning).topic()).isEqualTo(new GetLearnerTreeUseCase.TeachingTopic(readingTwo, "Trees: Reading", 2));
        // The writing chặng is the first of its own skill, whatever its place in the whole course.
        assertThat(node(opinion).topic().position()).isEqualTo(1);
    }

    @Test
    void listsTheLessonsThatTeachAPointInTheOrderOfTheirChangAndOfTheLesson() {
        when(content.getTopicSequence(true)).thenReturn(List.of(
                topic(readingOne, "Demo Reading", 900, LearningSkill.READING, point(matching, "PR_MATCHING_INFORMATION", "READING")),
                topic(readingTwo, "Trees: Reading", 955, LearningSkill.READING, point(scanning, "TR_R_SCANNING", "READING"))));
        LessonSummary second = lesson(readingOne, "Second", 20, matching);
        LessonSummary first = lesson(readingOne, "First", 10, matching, scanning);
        LessonSummary other = lesson(readingTwo, "Trees", 10, scanning);
        // Content may list them in any order; the tree orders them.
        when(content.getTopicLessons(readingOne)).thenReturn(List.of(second, first));
        when(content.getTopicLessons(readingTwo)).thenReturn(List.of(other));

        var lessons = node(matching).lessons();
        assertThat(lessons).extracting(GetLearnerTreeUseCase.TeachingLesson::title).containsExactly("First", "Second");
        assertThat(lessons).extracting(GetLearnerTreeUseCase.TeachingLesson::position).containsExactly(1, 2);
        assertThat(lessons.get(0).topicName()).isEqualTo("Demo Reading");
        assertThat(lessons.get(0).topicPosition()).isEqualTo(1);

        // A point can be taught in several chặng: the chặng come in course order.
        assertThat(node(scanning).lessons()).extracting(GetLearnerTreeUseCase.TeachingLesson::title).containsExactly("First", "Trees");
        assertThat(node(scanning).lessons().get(1).topicPosition()).isEqualTo(2);
    }

    @Test
    void stillAnswersWithoutLessonsWhenTheLessonsOfAChangCannotBeRead() {
        when(content.getTopicSequence(true)).thenReturn(List.of(
                topic(readingOne, "Demo Reading", 900, LearningSkill.READING, point(matching, "PR_MATCHING_INFORMATION", "READING"))));
        when(content.getTopicLessons(readingOne)).thenThrow(new IllegalStateException("content is down"));

        Node node = node(matching);
        assertThat(node.lessons()).isEmpty();
        assertThat(node.topic()).isNotNull();
    }

    @Test
    void givesAPointNoLessonsWhenNoneTeachesIt() {
        when(content.getTopicSequence(true)).thenReturn(List.of(
                topic(readingOne, "Demo Reading", 900, LearningSkill.READING, point(matching, "PR_MATCHING_INFORMATION", "READING"))));
        when(content.getTopicLessons(readingOne)).thenReturn(List.of());

        assertThat(node(matching).lessons()).isEmpty();
    }
}

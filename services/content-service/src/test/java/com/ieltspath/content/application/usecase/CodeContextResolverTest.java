package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.LessonAuthoringResult;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.LessonAuthoringSnapshot;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CodeContextResolverTest {
    private final CourseRepository courses = mock(CourseRepository.class);
    private final TopicRepository topics = mock(TopicRepository.class);
    private final LessonAuthoringRepository lessons = mock(LessonAuthoringRepository.class);
    private final CodeContextResolver resolver = new CodeContextResolver(courses, topics, lessons);

    @Test
    void lessonUsesParentTopicCodeAndTopicScopedCounter() {
        UUID topicId = UUID.randomUUID();
        Topic topic = mock(Topic.class);
        when(topic.getCode()).thenReturn("FOUNDATION_T01");
        when(topics.findById(topicId)).thenReturn(Optional.of(topic));

        var context = resolver.lesson(topicId);

        assertEquals("FOUNDATION_T01", context.values().get("topic"));
        assertEquals(topicId.toString(), context.scopeKey());
    }

    @Test
    void knowledgePointContextResolvesPrefixLevelAndNameWithoutDatabaseReads() {
        var context = resolver.knowledgePoint(KnowledgePointKind.GRAMMAR, Skill.ALL, 65, "Complex sentences");
        assertEquals("G", context.values().get("prefix"));
        assertEquals("65", context.values().get("level"));
        assertEquals("Complex sentences", context.values().get("name"));
        verifyNoInteractions(courses, topics, lessons);

        assertEquals("R", resolver.knowledgePoint(KnowledgePointKind.STRATEGY, Skill.READING, 55, "Reading").values().get("prefix"));
        assertEquals("L", resolver.knowledgePoint(KnowledgePointKind.STRATEGY, Skill.LISTENING, 55, "Listening").values().get("prefix"));
        assertEquals("W", resolver.knowledgePoint(KnowledgePointKind.STRATEGY, Skill.WRITING, 55, "Writing").values().get("prefix"));
        assertEquals("S", resolver.knowledgePoint(KnowledgePointKind.STRATEGY, Skill.SPEAKING, 55, "Speaking").values().get("prefix"));
        assertEquals("V", resolver.knowledgePoint(KnowledgePointKind.VOCABULARY, Skill.ALL, 60, "Words").values().get("prefix"));
    }

    @Test
    void packageUsesCourseIdBeforeTopicAndLessonAndScopesByType() {
        UUID explicitCourseId = UUID.randomUUID();
        UUID topicCourseId = UUID.randomUUID();
        Course course = mock(Course.class);
        when(course.getCode()).thenReturn("COURSE_EXPLICIT");
        when(courses.findById(explicitCourseId)).thenReturn(Optional.of(course));

        var context = resolver.packageContext(explicitCourseId, UUID.randomUUID(), UUID.randomUUID(), PackageType.PRACTICE_SET);

        assertEquals("COURSE_EXPLICIT", context.values().get("course"));
        assertEquals("PS", context.values().get("type"));
        assertEquals(explicitCourseId + ":PS", context.scopeKey());
        verifyNoInteractions(topics, lessons);
        assertNotEquals(topicCourseId, explicitCourseId);
    }

    @Test
    void packageInfersCourseFromTopicThenLessonAndFallsBackWhenNoCourseExists() {
        UUID topicId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Topic topic = mock(Topic.class);
        Course course = mock(Course.class);
        when(topic.getCourseId()).thenReturn(courseId);
        when(course.getCode()).thenReturn("COURSE_TOPIC");
        when(topics.findById(topicId)).thenReturn(Optional.of(topic));
        when(topics.findById(any())).thenAnswer(call -> Optional.of(topic));
        when(lessons.findSnapshotById(lessonId)).thenReturn(Optional.of(snapshot(lessonId, topicId)));
        when(courses.findById(courseId)).thenReturn(Optional.of(course));

        var fromTopic = resolver.packageContext(null, topicId, null, PackageType.MOCK_TEST);
        var fromLesson = resolver.packageContext(null, null, lessonId, PackageType.MOCK_TEST);
        assertEquals("COURSE_TOPIC", fromTopic.values().get("course"));
        assertEquals("COURSE_TOPIC", fromLesson.values().get("course"));

        when(topic.getCourseId()).thenReturn(null);
        var fallback = resolver.packageContext(null, topicId, null, PackageType.MOCK_TEST);
        assertFalse(fallback.courseResolved());
        assertEquals("NOCOURSE:MT", fallback.scopeKey());
        assertEquals("MT", fallback.values().get("type"));
    }

    private static LessonAuthoringSnapshot snapshot(UUID lessonId, UUID topicId) {
        return new LessonAuthoringSnapshot(lessonId, topicId, "L", "Lesson", null, null, null, 0,
                com.ieltspath.content.domain.vo.LessonStatus.DRAFT, null, null, java.util.List.of(), java.util.List.of());
    }
}

package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.CodeContext;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class CodeContextResolver {
    private final CourseRepository courses;
    private final TopicRepository topics;
    private final LessonAuthoringRepository lessons;

    public CodeContextResolver(CourseRepository courses, TopicRepository topics, LessonAuthoringRepository lessons) {
        this.courses = courses;
        this.topics = topics;
        this.lessons = lessons;
    }

    public CodeContext lesson(UUID topicId) {
        var topic = topics.findById(topicId).orElseThrow(() -> new TopicNotFoundException(topicId));
        return new CodeContext(Map.of("topic", topic.getCode()), topicId.toString(), true);
    }

    public CodeContext knowledgePoint(KnowledgePointKind kind, Skill skill, Integer level, String name) {
        Map<String, String> values = new java.util.HashMap<>();
        String prefix = knowledgePointPrefix(kind, skill);
        if (!prefix.isBlank()) values.put("prefix", prefix);
        if (level != null) values.put("level", Integer.toString(level));
        if (name != null) values.put("name", name);
        return new CodeContext(Map.copyOf(values), "", false);
    }

    private static String knowledgePointPrefix(KnowledgePointKind kind, Skill skill) {
        if (kind == null) return "";
        if (kind == KnowledgePointKind.GRAMMAR && skill == Skill.ALL) return "G";
        if (kind == KnowledgePointKind.VOCABULARY && skill == Skill.ALL) return "V";
        if (skill == null) return "";
        return switch (skill) {
            case LISTENING -> "L";
            case READING -> "R";
            case WRITING -> "W";
            case SPEAKING -> "S";
            case ALL -> "";
        };
    }

    public CodeContext packageContext(UUID courseId, UUID topicId, UUID lessonId, PackageType packageType) {
        if (packageType == null) return new CodeContext(Map.of(), "", false);
        UUID resolvedCourseId = courseId;
        if (resolvedCourseId == null && topicId != null) {
            var topic = topics.findById(topicId).orElseThrow(() -> new TopicNotFoundException(topicId));
            resolvedCourseId = topic.getCourseId();
        }
        if (resolvedCourseId == null && lessonId != null) {
            var lesson = lessons.findSnapshotById(lessonId).orElseThrow(() -> new LessonNotFoundException(lessonId));
            var topic = topics.findById(lesson.topicId()).orElseThrow(() -> new TopicNotFoundException(lesson.topicId()));
            resolvedCourseId = topic.getCourseId();
        }

        String abbreviation = packageType.codeAbbreviation();
        if (resolvedCourseId == null) {
            return new CodeContext(Map.of("type", abbreviation), "NOCOURSE:" + abbreviation, false);
        }
        UUID courseKey = resolvedCourseId;
        var course = courses.findById(courseKey).orElseThrow(() -> new CourseNotFoundException(courseKey));
        return new CodeContext(Map.of("course", course.getCode(), "type", abbreviation),
                resolvedCourseId + ":" + abbreviation, true);
    }
}

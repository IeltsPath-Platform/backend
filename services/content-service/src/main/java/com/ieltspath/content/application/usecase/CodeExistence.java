package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CodeExistence {
    private final CourseRepository courses;
    private final TopicRepository topics;
    private final LessonAuthoringRepository lessons;
    private final ContentPackageRepository packages;
    private final KnowledgePointRepository knowledgePoints;

    public CodeExistence(CourseRepository courses, TopicRepository topics, LessonAuthoringRepository lessons,
                         ContentPackageRepository packages) {
        this(courses, topics, lessons, packages, null);
    }

    @Autowired
    public CodeExistence(CourseRepository courses, TopicRepository topics, LessonAuthoringRepository lessons,
                         ContentPackageRepository packages, KnowledgePointRepository knowledgePoints) {
        this.courses = courses;
        this.topics = topics;
        this.lessons = lessons;
        this.packages = packages;
        this.knowledgePoints = knowledgePoints;
    }

    public boolean exists(String kind, String code) {
        return switch (kind) {
            case "COURSE" -> courses.existsByCode(code);
            case "TOPIC" -> topics.existsByCode(code);
            case "LESSON" -> lessons.existsByCode(code);
            case "PACKAGE" -> packages.existsByCode(code);
            case "KNOWLEDGE_POINT" -> knowledgePoints.existsByCode(code);
            default -> throw new IllegalArgumentException("Loại bộ mã này chưa được kết nối với luồng tạo.");
        };
    }
}

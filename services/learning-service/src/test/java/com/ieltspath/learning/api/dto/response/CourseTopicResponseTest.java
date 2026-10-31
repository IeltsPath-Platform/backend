package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.CourseResult;
import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CourseTopicResponseTest {
    @Test
    void courseResponseIncludesCoverAndDescription() {
        var result = new CourseResult(UUID.randomUUID(), "IELTS_6_5", "IELTS 6.5", new BigDecimal("6.5"),
                2, 1, false, "LOCKED", null, 1, 4, null, "https://cdn.example/course.jpg", "Course overview");

        var response = CourseResponse.from(result);

        assertEquals("https://cdn.example/course.jpg", response.coverImageUrl());
        assertEquals("Course overview", response.description());
    }

    @Test
    void topicResponseIncludesDescriptionAndCourseCoverMetadata() {
        var course = new TopicResult.Course(UUID.randomUUID(), "IELTS_6_5", "IELTS 6.5", new BigDecimal("6.5"),
                "https://cdn.example/course.jpg", "Course overview");
        var result = new TopicResult(UUID.randomUUID(), "READING_6_5", "Reading 6.5", 1, TopicStatus.LOCKED,
                0, "PREMIUM_CONTENT", null, true, course, true, Set.of(), 2, "Build inference skills");

        var response = TopicResponse.from(result);

        assertEquals("Build inference skills", response.description());
        assertEquals("https://cdn.example/course.jpg", response.course().coverImageUrl());
        assertEquals("Course overview", response.course().description());
    }

    @Test
    void legacyResultConstructorsLeaveNewFieldsNull() {
        var course = new CourseResult(UUID.randomUUID(), "IELTS_6_5", "IELTS 6.5", new BigDecimal("6.5"),
                2, 1, false, "LOCKED", null, 1, 4, null);
        var topicCourse = new TopicResult.Course(UUID.randomUUID(), "IELTS_6_5", "IELTS 6.5",
                new BigDecimal("6.5"));
        var topic = new TopicResult(UUID.randomUUID(), "READING_6_5", "Reading 6.5", 1, TopicStatus.LOCKED,
                0, "PREMIUM_CONTENT", null, true, topicCourse, true, Set.of(), 2);

        assertNull(CourseResponse.from(course).coverImageUrl());
        assertNull(CourseResponse.from(course).description());
        assertNull(TopicResponse.from(topic).description());
        assertNull(TopicResponse.from(topic).course().coverImageUrl());
        assertNull(TopicResponse.from(topic).course().description());
    }
}

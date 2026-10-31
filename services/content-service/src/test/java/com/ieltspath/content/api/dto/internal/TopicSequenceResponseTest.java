package com.ieltspath.content.api.dto.internal;

import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TopicSequenceResponseTest {
    @Test
    void includesCourseCoverAndDescriptionsInInternalTopicSequence() {
        var course = new TopicSequenceResult.CourseEntry(UUID.randomUUID(), "IELTS_6_5", "IELTS 6.5",
                new BigDecimal("6.5"), true, true, "https://cdn.example/course.jpg", "Course overview");
        var result = new TopicSequenceResult(UUID.randomUUID(), "READING_6_5", "Reading 6.5", 1,
                "PREMIUM_CONTENT", List.of(), Skill.READING, true, course, List.of(Skill.READING), true, 2,
                "Build inference skills");

        var response = TopicSequenceResponse.from(result);

        assertThat(response.description()).isEqualTo("Build inference skills");
        assertThat(response.course().coverImageUrl()).isEqualTo("https://cdn.example/course.jpg");
        assertThat(response.course().description()).isEqualTo("Course overview");
    }

    @Test
    void oldResultConstructorsLeaveNewInternalFieldsNull() {
        var course = new TopicSequenceResult.CourseEntry(UUID.randomUUID(), "IELTS_6_5", "IELTS 6.5",
                new BigDecimal("6.5"), true, true);
        var result = new TopicSequenceResult(UUID.randomUUID(), "READING_6_5", "Reading 6.5", 1,
                "PREMIUM_CONTENT", List.of(), Skill.READING, true, course, List.of(Skill.READING), true, 2);

        var response = TopicSequenceResponse.from(result);

        assertThat(response.description()).isNull();
        assertThat(response.course().coverImageUrl()).isNull();
        assertThat(response.course().description()).isNull();
    }
}

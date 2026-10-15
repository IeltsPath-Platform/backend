package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.Skill;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Complete published lesson denominator; never includes questions or answers. */
public record ProgressMetadataResult(String revision, boolean complete, List<Lesson> lessons) {
    public record Lesson(UUID courseId, String courseName, BigDecimal bandLevel, UUID topicId,
                         UUID lessonId, List<Skill> skills) {}
}

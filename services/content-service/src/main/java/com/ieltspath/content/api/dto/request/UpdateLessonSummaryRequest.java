package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.LessonSummaryMarkdown;
import jakarta.validation.constraints.Size;

/** Blank or missing {@code markdown} removes the summary. */
public record UpdateLessonSummaryRequest(@Size(max = LessonSummaryMarkdown.MAX_LENGTH) String markdown) {}

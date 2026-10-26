package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateLessonRequest(@NotBlank @Size(max = 255) String title, String summary, String summaryMarkdown,
                                  @Positive Integer estimatedMinutes) {}

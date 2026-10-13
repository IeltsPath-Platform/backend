package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.LessonResourceType;
import jakarta.validation.constraints.*;

public record CreateLessonResourceRequest(@NotNull LessonResourceType type,
                                          @NotBlank @Size(max = 255) String title,
                                          @NotBlank @Size(max = 2000) String url,
                                          @NotNull @Min(0) Integer sortOrder) {}

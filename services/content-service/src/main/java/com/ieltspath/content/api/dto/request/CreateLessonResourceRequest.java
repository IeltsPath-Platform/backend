package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.LessonResourceType;
import jakarta.validation.constraints.*;

/** Give a {@code url}, or for a document the {@code fileKey} returned by the document upload; not both. */
public record CreateLessonResourceRequest(@NotNull LessonResourceType type,
                                          @NotBlank @Size(max = 255) String title,
                                          @Size(max = 2000) String url,
                                          @Size(max = 300) String fileKey,
                                          @NotNull @Min(0) Integer sortOrder) {}

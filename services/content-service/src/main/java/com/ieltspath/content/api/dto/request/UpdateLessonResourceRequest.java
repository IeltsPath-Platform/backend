package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.*;

/**
 * The type of a resource is fixed; to move a link between slides and documents, delete and re-create it. A file
 * resource keeps its file, so it is updated without a {@code url}.
 */
public record UpdateLessonResourceRequest(@NotBlank @Size(max = 255) String title,
                                          @Size(max = 2000) String url,
                                          @NotNull @Min(0) Integer sortOrder) {}

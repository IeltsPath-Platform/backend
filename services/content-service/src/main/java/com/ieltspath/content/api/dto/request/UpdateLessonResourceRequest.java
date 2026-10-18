package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.*;

/** The type of a resource is fixed; to move a link between slides and documents, delete and re-create it. */
public record UpdateLessonResourceRequest(@NotBlank @Size(max = 255) String title,
                                          @NotBlank @Size(max = 2000) String url,
                                          @NotNull @Min(0) Integer sortOrder) {}

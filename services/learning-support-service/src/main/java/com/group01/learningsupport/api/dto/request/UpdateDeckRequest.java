package com.group01.learningsupport.api.dto.request;

import com.group01.learningsupport.domain.vo.LibraryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDeckRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull LibraryStatus status
) {
}

package com.group01.community.api.dto.request;

import com.group01.community.domain.vo.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotNull PostCategory category,
        @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String body
) {
}

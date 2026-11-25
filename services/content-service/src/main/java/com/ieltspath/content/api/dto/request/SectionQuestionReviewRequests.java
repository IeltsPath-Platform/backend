package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class SectionQuestionReviewRequests {
    private SectionQuestionReviewRequests() {}
    public record Patch(@NotNull Integer revision, @Size(max=400) String stem, @Size(max=4000) String explanation,
                        List<@Size(max=4000) String> accepted, String correct,
                        List<Option> options) {
        public record Option(@NotNull @Size(max=20) String optionKey, @NotNull @Size(max=4000) String content) {}
    }
    public record Approve(@NotEmpty List<UUID> itemIds, @NotNull Integer revision, boolean acceptSourceChange) {}
}

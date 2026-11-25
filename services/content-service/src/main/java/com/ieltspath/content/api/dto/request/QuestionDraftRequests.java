package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.application.command.QuestionDraftItemPatch;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuestionDraftRequests {

    private QuestionDraftRequests() {
    }

    public record Request(
            @NotNull UUID knowledgePointId,
            @NotNull QuestionType questionType,
            @Min(1) int count,
            QuestionDifficulty difficulty,
            @Size(max = 500, message = "Hướng dẫn thêm không được dài quá 500 ký tự") String instructions,
            UUID lessonId,
            UUID blockId
    ) {
        public Request(UUID knowledgePointId, QuestionType questionType, int count, QuestionDifficulty difficulty,
                       String instructions) {
            this(knowledgePointId, questionType, count, difficulty, instructions, null, null);
        }
    }

    /** Fields left out stay as they are; see {@code QuestionDraftItemPatch}. */
    public record ItemPatch(
            @Size(max = 4000) String stem,
            @Size(max = 4000) String explanation,
            @Size(max = 6) List<@Valid Option> options,
            @Size(max = 100) String correct,
            Map<@Size(max = 100) String, @Size(max = 100) String> distractors,
            @Size(max = 20) List<@Size(max = 500) String> accepted
    ) {
        public record Option(@NotNull @Size(max = 100) String optionKey, @NotNull @Size(max = 4000) String content) {
        }

        public QuestionDraftItemPatch toCommand() {
            return new QuestionDraftItemPatch(stem, explanation,
                    options == null ? null : options.stream()
                            .map(option -> new QuestionDraftItemPatch.Option(option.optionKey(), option.content()))
                            .toList(),
                    correct, distractors, accepted);
        }
    }

    /** Stable item identifiers and the revision shown to the author. */
    public record Approve(@NotEmpty List<UUID> itemIds, @Min(0) int revision, boolean createNewBlock) {
        public Approve(List<UUID> itemIds, int revision) { this(itemIds, revision, false); }
    }
}

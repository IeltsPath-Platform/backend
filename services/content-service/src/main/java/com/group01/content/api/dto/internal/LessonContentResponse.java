package com.group01.content.api.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;

import java.util.List;
import java.util.UUID;

/** Internal lesson payload; it carries answer specs and must never be proxied to a learner unchanged. */
public record LessonContentResponse(
        UUID lessonId,
        UUID topicId,
        String code,
        String title,
        String summary,
        int sortOrder,
        List<UUID> knowledgePointIds,
        List<Block> blocks
) {
    /** A block shows only the field of its type; {@code blockKind} appears on EXERCISE blocks. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Block(
            UUID blockId,
            BlockType blockType,
            LessonBlockKind blockKind,
            int sortOrder,
            String textContent,
            Asset asset,
            List<UUID> vocabularySenseIds,
            List<Question> questions
    ) {}

    public record Asset(UUID id, AssetType assetType, String textContent, String mediaReference,
                        Integer durationSeconds) {}

    /** {@code options} is null for a fill question; {@code answerSpec} follows answer spec v1. */
    public record Question(
            UUID questionVersionId,
            int sortOrder,
            String stem,
            @JsonRawValue String options,
            @JsonRawValue String answerSpec,
            String explanation,
            List<UUID> knowledgePointIds
    ) {}

    public static LessonContentResponse from(LessonContentResult result) {
        return new LessonContentResponse(result.lessonId(), result.topicId(), result.code(), result.title(),
                result.summary(), result.sortOrder(), result.knowledgePointIds(),
                result.blocks().stream().map(LessonContentResponse::block).toList());
    }

    private static Block block(LessonContentResult.Block block) {
        LessonContentResult.Asset asset = block.asset();
        return new Block(block.blockId(), block.blockType(), block.blockKind(), block.sortOrder(), block.textContent(),
                asset == null ? null : new Asset(asset.id(), asset.assetType(), asset.textContent(),
                        asset.mediaReference(), asset.durationSeconds()),
                block.vocabularySenseIds(),
                block.questions() == null ? null : block.questions().stream()
                        .map(q -> new Question(q.questionVersionId(), q.sortOrder(), q.stem(), q.optionsJson(),
                                q.answerSpecJson(), q.explanation(), q.knowledgePointIds()))
                        .toList());
    }
}

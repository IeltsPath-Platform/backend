package com.group01.content.api.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;
import com.group01.content.domain.vo.Skill;

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
        List<Block> blocks,
        Skill skill,
        List<Skill> skills
) {
    /**
     * A block shows only the field of its type; {@code blockKind} appears on EXERCISE blocks.
     * {@code knowledgePointIds} are those a TEXT block teaches or an EXERCISE block's questions measure.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Block(
            UUID blockId,
            BlockType blockType,
            LessonBlockKind blockKind,
            int sortOrder,
            String textContent,
            Asset asset,
            List<UUID> vocabularySenseIds,
            List<Question> questions,
            List<UUID> knowledgePointIds
    ) {}

    /** For AUDIO, {@code textContent} is the transcript and {@code mediaUrl} the playable URL. */
    public record Asset(UUID id, AssetType assetType, String textContent, String mediaReference,
                        Integer durationSeconds, String mediaUrl) {}

    /**
     * {@code options} is null for a fill question; {@code answerSpec} follows answer spec v1. {@code assets} appears
     * only on questions of an essay block.
     */
    public record Question(
            UUID questionVersionId,
            int sortOrder,
            String stem,
            @JsonRawValue String options,
            @JsonRawValue String answerSpec,
            String explanation,
            String hint,
            List<UUID> knowledgePointIds,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<QuestionAsset> assets
    ) {}

    public record QuestionAsset(UUID assetId, AssetType assetType, String mediaUrl, String altText, int sortOrder) {}

    public static LessonContentResponse from(LessonContentResult result) {
        return new LessonContentResponse(result.lessonId(), result.topicId(), result.code(), result.title(),
                result.summary(), result.sortOrder(), result.knowledgePointIds(),
                result.blocks().stream().map(LessonContentResponse::block).toList(), result.skill(), result.skills());
    }

    private static Block block(LessonContentResult.Block block) {
        LessonContentResult.Asset asset = block.asset();
        return new Block(block.blockId(), block.blockType(), block.blockKind(), block.sortOrder(), block.textContent(),
                asset == null ? null : new Asset(asset.id(), asset.assetType(), asset.textContent(),
                        asset.mediaReference(), asset.durationSeconds(), asset.mediaUrl()),
                block.vocabularySenseIds(),
                block.questions() == null ? null : block.questions().stream()
                        .map(q -> new Question(q.questionVersionId(), q.sortOrder(), q.stem(), q.optionsJson(),
                                q.answerSpecJson(), q.explanation(), q.hint(), q.knowledgePointIds(),
                                q.assets() == null ? null : q.assets().stream()
                                        .map(a -> new QuestionAsset(a.assetId(), a.assetType(), a.mediaUrl(),
                                                a.altText(), a.sortOrder()))
                                        .toList()))
                        .toList(),
                block.knowledgePointIds());
    }
}

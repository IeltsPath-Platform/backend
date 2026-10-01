package com.group01.content.application.result;

import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;

import java.util.List;
import java.util.UUID;

/**
 * A published lesson with its blocks in order. Exercise questions carry the answer spec and explanation, so this is
 * for services only and must never reach a learner unchanged. JSON values ({@code optionsJson},
 * {@code answerSpecJson}) are kept as stored.
 */
public record LessonContentResult(
        UUID lessonId,
        UUID topicId,
        String code,
        String title,
        String summary,
        int sortOrder,
        List<UUID> knowledgePointIds,
        List<Block> blocks
) {
    public LessonContentResult withBlocks(List<Block> newBlocks) {
        return new LessonContentResult(lessonId, topicId, code, title, summary, sortOrder, knowledgePointIds, newBlocks);
    }

    /**
     * Only the field matching {@code blockType} is set; the others are null. {@code blockKind} is set for
     * {@code EXERCISE} blocks once the use case has classified them.
     */
    public record Block(
            UUID blockId,
            BlockType blockType,
            LessonBlockKind blockKind,
            int sortOrder,
            String textContent,
            Asset asset,
            List<UUID> vocabularySenseIds,
            List<Question> questions
    ) {
        public Block withBlockKind(LessonBlockKind kind) {
            return new Block(blockId, blockType, kind, sortOrder, textContent, asset, vocabularySenseIds, questions);
        }
    }

    public record Asset(UUID id, AssetType assetType, String textContent, String mediaReference,
                        Integer durationSeconds) {}

    /** {@code specType} and {@code specPassBand} are read from the answer spec only to classify the block. */
    public record Question(
            UUID questionVersionId,
            int sortOrder,
            String stem,
            String optionsJson,
            String answerSpecJson,
            String explanation,
            List<UUID> knowledgePointIds,
            String specType,
            String specPassBand
    ) {}
}

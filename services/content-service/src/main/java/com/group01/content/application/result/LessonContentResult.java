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

        public Block withAsset(Asset newAsset) {
            return new Block(blockId, blockType, blockKind, sortOrder, textContent, newAsset, vocabularySenseIds,
                    questions);
        }

        public Block withQuestions(List<Question> newQuestions) {
            return new Block(blockId, blockType, blockKind, sortOrder, textContent, asset, vocabularySenseIds,
                    newQuestions);
        }
    }

    /** {@code mediaUrl} is set for media assets once the use case has checked the stored reference. */
    public record Asset(UUID id, AssetType assetType, String textContent, String mediaReference,
                        Integer durationSeconds, String mediaUrl) {
        public Asset withMediaUrl(String url) {
            return new Asset(id, assetType, textContent, mediaReference, durationSeconds, url);
        }
    }

    /**
     * {@code spec} holds the answer-spec facts used to classify the block. {@code assets} are the images attached to
     * the question version; {@code mediaUrl} is set once the use case has checked the stored reference.
     */
    public record Question(
            UUID questionVersionId,
            int sortOrder,
            String stem,
            String optionsJson,
            String answerSpecJson,
            String explanation,
            List<UUID> knowledgePointIds,
            LessonBlockKind.QuestionSpec spec,
            List<QuestionAsset> assets
    ) {
        public Question withAssets(List<QuestionAsset> newAssets) {
            return new Question(questionVersionId, sortOrder, stem, optionsJson, answerSpecJson, explanation,
                    knowledgePointIds, spec, newAssets);
        }
    }

    public record QuestionAsset(UUID assetId, AssetType assetType, String mediaReference, String mediaUrl,
                                String altText, int sortOrder) {
        public QuestionAsset withMediaUrl(String url) {
            return new QuestionAsset(assetId, assetType, mediaReference, url, altText, sortOrder);
        }
    }
}

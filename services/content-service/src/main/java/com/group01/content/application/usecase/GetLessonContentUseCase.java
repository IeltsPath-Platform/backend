package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.domain.exception.LessonNotFoundException;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;
import com.group01.content.domain.vo.MediaReferencePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetLessonContentUseCase {
    private final LearningContentReader reader;
    private final MediaReferencePolicy mediaPolicy;

    public GetLessonContentUseCase(LearningContentReader reader, MediaReferencePolicy mediaPolicy) {
        this.reader = reader;
        this.mediaPolicy = mediaPolicy;
    }

    public LessonContentResult execute(UUID lessonId) {
        LessonContentResult lesson = reader.publishedLesson(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
        return lesson.withBlocks(lesson.blocks().stream().map(this::resolved).toList());
    }

    private LessonContentResult.Block resolved(LessonContentResult.Block block) {
        if (block.blockType() == BlockType.ASSET && block.asset().assetType() != AssetType.PASSAGE) {
            LessonContentResult.Asset asset = block.asset();
            return block.withAsset(asset.withMediaUrl(mediaPolicy.resolve(asset.assetType(), asset.mediaReference())));
        }
        if (block.blockType() != BlockType.EXERCISE) {
            return block;
        }
        // Exercise blocks get their kind; only essay questions keep their (checked) images.
        LessonBlockKind kind = LessonBlockKind.classify(
                block.questions().stream().map(LessonContentResult.Question::spec).toList());
        return block.withBlockKind(kind).withQuestions(block.questions().stream()
                .map(q -> q.withAssets(kind == LessonBlockKind.ESSAY
                        ? q.assets().stream()
                        .map(a -> a.withMediaUrl(mediaPolicy.resolve(a.assetType(), a.mediaReference())))
                        .toList()
                        : null))
                .toList());
    }
}

package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.AccessTier;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonBlockKind;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetLessonContentUseCase {
    private final LearningContentReader reader;
    private final MediaReferencePolicy mediaPolicy;
    private final MediaUrlResolver mediaUrls;

    @Autowired
    public GetLessonContentUseCase(LearningContentReader reader, MediaReferencePolicy mediaPolicy,
                                  MediaUrlResolver mediaUrls) {
        this.reader = reader;
        this.mediaPolicy = mediaPolicy;
        this.mediaUrls = mediaUrls;
    }

    public GetLessonContentUseCase(LearningContentReader reader, MediaReferencePolicy mediaPolicy) {
        this.reader = reader;
        this.mediaPolicy = mediaPolicy;
        this.mediaUrls = null;
    }

    public LessonContentResult execute(UUID lessonId) {
        return execute(lessonId, false);
    }

    public LessonContentResult execute(UUID lessonId, boolean signedAudio) {
        LessonContentResult lesson = load(lessonId);
        return lesson.withBlocks(lesson.blocks().stream().map(block -> resolved(block, signedAudio)).toList());
    }

    public LessonContentResult accessSummary(UUID lessonId) {
        return load(lessonId);
    }

    private LessonContentResult load(UUID lessonId) {
        return reader.publishedLesson(lessonId).orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    private LessonContentResult.Block resolved(LessonContentResult.Block block, boolean signedAudio) {
        if (block.blockType() == BlockType.ASSET && block.asset().assetType() != AssetType.PASSAGE) {
            LessonContentResult.Asset asset = block.asset();
            String url = signedAudio && mediaUrls != null
                    ? mediaUrls.resolve(asset.assetType(), asset.mediaReference(), asset.accessTier(), true)
                    : mediaPolicy.resolve(asset.assetType(), asset.mediaReference());
            return block.withAsset(asset.withMediaUrl(url));
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

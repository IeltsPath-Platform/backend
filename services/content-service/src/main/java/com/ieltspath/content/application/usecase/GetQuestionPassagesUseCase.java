package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.QuestionPassageReader;
import com.ieltspath.content.application.port.QuestionPassageReader.Passage;
import com.ieltspath.content.domain.vo.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** The passages and recordings that go with a question version, so an author can see the question in its context. */
@Service
@Transactional(readOnly = true)
public class GetQuestionPassagesUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetQuestionPassagesUseCase.class);

    private final QuestionPassageReader reader;
    private final MediaUrlResolver mediaUrls;

    public GetQuestionPassagesUseCase(QuestionPassageReader reader, MediaUrlResolver mediaUrls) {
        this.reader = reader;
        this.mediaUrls = mediaUrls;
    }

    public List<Passage> execute(UUID questionVersionId) {
        return reader.passagesOf(questionVersionId).stream().map(this::withPlayableUrl).toList();
    }

    /**
     * A recording whose link cannot be made (a bad key, storage unavailable) keeps its transcript and goes without a
     * link, so one broken file does not hide the other assets of the question. Private audio gets a short-lived signed
     * link; it is not stored.
     */
    private Passage withPlayableUrl(Passage passage) {
        if (passage.type() != AssetType.AUDIO && passage.type() != AssetType.IMAGE) return passage;
        try {
            return passage.withMediaUrl(mediaUrls.resolve(passage.type(), passage.mediaReference(), passage.tier(), true));
        } catch (RuntimeException failed) {
            log.warn("Could not make a playable link for an audio asset of a question: {}", failed.getClass().getSimpleName());
            return passage;
        }
    }
}

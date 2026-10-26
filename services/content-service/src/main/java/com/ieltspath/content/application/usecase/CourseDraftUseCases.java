package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Creates, edits and checks the author's own course drafts. A handwritten draft is ready at once and does not use the
 * AI allowance; one that the AI made is edited the same way.
 */
@Service
public class CourseDraftUseCases {

    private final AiDraftRepository drafts;
    private final CourseDraftCodec codec;
    private final CourseDraftAnalysisService analysis;
    private final Clock clock;

    public CourseDraftUseCases(AiDraftRepository drafts, CourseDraftCodec codec, CourseDraftAnalysisService analysis,
                               Clock clock) {
        this.drafts = drafts;
        this.codec = codec;
        this.analysis = analysis;
        this.clock = clock;
    }

    /** {@code structure} is the JSON sent by the client, or null for an empty draft. */
    @Transactional
    public AiDraft create(UUID author, JsonNode structure) {
        CourseDraftStructure plan = structure == null || structure.isNull() ? CourseDraftStructure.empty()
                : codec.parse(structure);
        return drafts.save(AiDraft.manual(DraftKind.COURSE, author, codec.write(plan), clock.instant()));
    }

    @Transactional
    public AiDraft save(UUID author, UUID draftId, JsonNode structure) {
        AiDraft draft = ownReadyDraft(author, draftId);
        draft.replaceResult(codec.write(codec.parse(structure)));
        return drafts.save(draft);
    }

    @Transactional(readOnly = true)
    public CourseDraftAnalysis analyze(UUID author, UUID draftId) {
        AiDraft draft = ownReadyDraft(author, draftId);
        return analysis.analyze(codec.read(draft.getResultJson())).analysis();
    }

    /** The author's own course draft, which must still be waiting for review. Another author's is not found. */
    AiDraft ownReadyDraft(UUID author, UUID draftId) {
        AiDraft draft = drafts.findById(draftId)
                .filter(found -> found.getRequestedBy().equals(author) && found.getKind() == DraftKind.COURSE)
                .orElseThrow(() -> new AiDraftNotFoundException(draftId));
        if (draft.getStatus() != DraftStatus.READY) {
            throw new InvalidContentStateException("Draft is " + draft.getStatus() + ", expected READY");
        }
        return draft;
    }
}

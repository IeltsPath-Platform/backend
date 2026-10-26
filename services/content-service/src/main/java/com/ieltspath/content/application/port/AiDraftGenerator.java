package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;

/**
 * Turns one queued draft into its result. One implementation per {@link DraftKind}; the job picks by kind. A failure
 * is thrown as {@link ContentGenerationException} so the job can record its code.
 */
public interface AiDraftGenerator {
    DraftKind kind();

    GenerationOutcome generate(AiDraft draft);
}

package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.AiDraftOrigin;
import com.ieltspath.content.domain.aggregate.AiDraft;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AiDraftOriginReader {
    Map<UUID, AiDraftOrigin> findOrigins(List<AiDraft> drafts);
}

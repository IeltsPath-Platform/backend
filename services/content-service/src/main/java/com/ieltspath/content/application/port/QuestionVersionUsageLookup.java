package com.ieltspath.content.application.port;

import java.util.UUID;

public interface QuestionVersionUsageLookup {
    boolean isUsedByPublishedContent(UUID questionVersionId);
}

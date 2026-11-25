package com.ieltspath.content.application.port;

/** Request limits needed by the question draft use case. */
public interface QuestionDraftLimits {
    int maxItemsPerRequest();
    int dailyLimit();
}

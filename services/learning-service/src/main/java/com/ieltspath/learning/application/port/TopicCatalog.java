package com.ieltspath.learning.application.port;

import java.util.List;

/** Topics for display only; never decide access, price or a stored learning value from this catalog. */
public interface TopicCatalog {
    List<LearningContentClient.Topic> topics(boolean includeErrorTraps);

    /** Reads Content on every call. */
    static TopicCatalog direct(LearningContentClient content) {
        return includeErrorTraps -> includeErrorTraps ? content.getTopicSequence(true) : content.getTopicSequence();
    }
}

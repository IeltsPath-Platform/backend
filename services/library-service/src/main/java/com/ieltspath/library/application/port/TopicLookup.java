package com.ieltspath.library.application.port;

import java.util.UUID;

public interface TopicLookup {
    boolean exists(UUID topicId);
}

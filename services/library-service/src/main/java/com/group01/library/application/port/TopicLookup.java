package com.group01.library.application.port;

import java.util.UUID;

public interface TopicLookup {
    boolean exists(UUID topicId);
}

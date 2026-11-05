package com.ieltspath.library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** Library may be called by Content but must not call Content back: the two services stay one-way. */
class ServiceBoundaryTest {
    @Test
    void libraryHasNoClientForContentTopics() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.ieltspath.library.infrastructure.client.ContentTopicClient"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.ieltspath.library.application.port.TopicLookup"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.ieltspath.library.application.exception.TopicServiceUnavailableException"));
    }
}

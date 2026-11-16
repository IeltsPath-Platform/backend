package com.ieltspath.library.infrastructure.messaging;

import com.ieltspath.events.library.VocabularyReviewDueTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Declares the library exchange and the review-due queue topology. Notification declares the same topology, so the queue
 * exists whichever service starts first and the relay's mandatory publish always has a route.
 */
@Configuration
@EnableScheduling
public class LibraryMessagingConfig {

    @Bean
    Declarables vocabularyReviewDueTopology() {
        return VocabularyReviewDueTopology.declarables();
    }
}

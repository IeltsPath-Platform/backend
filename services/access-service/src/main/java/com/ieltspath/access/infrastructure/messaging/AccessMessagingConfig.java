package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionGrantedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the access exchange and the subscription-granted queue topology. Notification declares the same topology, so
 * the queue exists whichever service starts first.
 */
@Configuration
public class AccessMessagingConfig {

    @Bean
    Declarables subscriptionGrantedTopology() {
        return SubscriptionGrantedTopology.declarables(AccessMessaging.EXCHANGE);
    }
}

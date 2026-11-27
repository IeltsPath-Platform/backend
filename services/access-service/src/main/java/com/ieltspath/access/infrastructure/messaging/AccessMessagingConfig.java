package com.ieltspath.access.infrastructure.messaging;

import com.ieltspath.events.access.AccessMessaging;
import com.ieltspath.events.access.SubscriptionExpiryNoticeTopology;
import com.ieltspath.events.access.SubscriptionGrantedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the access exchange and, for each event it relays, one Notification queue bound to it (the grant queue and the
 * expiry mail queue). Notification declares the same topologies, so those queues exist whichever service starts first;
 * without them a relayed event is returned as unroutable and given up after the last attempt. The in-app expiry queue is
 * declared by Notification only.
 */
@Configuration
public class AccessMessagingConfig {

    @Bean
    Declarables subscriptionGrantedTopology() {
        return SubscriptionGrantedTopology.declarables(AccessMessaging.EXCHANGE);
    }

    @Bean
    Declarables subscriptionExpiryNoticeTopology() {
        return SubscriptionExpiryNoticeTopology.declarables();
    }
}

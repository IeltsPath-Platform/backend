package com.ieltspath.user.infrastructure.messaging;

import com.ieltspath.user.domain.aggregate.OutboxEvent;

public interface UserEventPublisher {
    void publish(OutboxEvent event);
}

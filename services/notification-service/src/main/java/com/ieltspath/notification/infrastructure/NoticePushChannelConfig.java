package com.ieltspath.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Subscribes this instance to the notice push channel. The container does not start with the context, so a Redis outage
 * never stops the service; {@link NoticePushSupervisor} keeps retrying the subscription.
 */
@Configuration
class NoticePushChannelConfig {
    @Bean
    RedisMessageListenerContainer noticePushChannelContainer(RedisConnectionFactory connections, NoticePushBridge bridge) {
        var container = new RedisMessageListenerContainer() {
            @Override
            public boolean isAutoStartup() {
                return false;
            }
        };
        container.setConnectionFactory(connections);
        container.addMessageListener(bridge, new ChannelTopic(NoticePushBridge.CHANNEL));
        return container;
    }
}

@Component
class NoticePushSupervisor {
    private static final Logger log = LoggerFactory.getLogger(NoticePushSupervisor.class);

    private final RedisMessageListenerContainer container;

    NoticePushSupervisor(RedisMessageListenerContainer container) {
        this.container = container;
    }

    @Scheduled(fixedDelayString = "${notification.push.resubscribe-ms:5000}",
            initialDelayString = "${notification.push.resubscribe-ms:5000}")
    void ensureSubscribed() {
        if (container.isRunning()) return;
        try {
            container.start();
        } catch (RuntimeException unavailable) {
            log.warn("Notice push channel unavailable, retrying: errorType={}", unavailable.getClass().getSimpleName());
        }
    }
}

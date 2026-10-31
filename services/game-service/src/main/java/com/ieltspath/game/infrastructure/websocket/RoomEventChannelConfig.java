package com.ieltspath.game.infrastructure.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Subscribes this instance to the room event channel. The container does not start with the context: Redis being down
 * must not stop the service, so {@link RoomEventChannelSupervisor} keeps trying to subscribe.
 */
@Configuration
class RoomEventChannelConfig {
    @Bean
    RedisMessageListenerContainer roomEventChannelContainer(RedisConnectionFactory connections, RoomEventBridge bridge) {
        var container = new RedisMessageListenerContainer() {
            @Override
            public boolean isAutoStartup() {
                return false;
            }
        };
        container.setConnectionFactory(connections);
        container.addMessageListener(bridge, new ChannelTopic(RoomEventBridge.CHANNEL));
        return container;
    }
}

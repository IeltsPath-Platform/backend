package com.ieltspath.game.infrastructure.messaging;

import com.ieltspath.events.user.UserMessaging;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDeletionMessagingConfig {
    public static final String QUEUE = "game.user-deleted.v1";
    public static final String RETRY_EXCHANGE = "game.user-deleted.retry";
    public static final String RETRY_QUEUE = "game.user-deleted.v1.retry";
    public static final String DEAD_LETTER_EXCHANGE = "game.user-deleted.dlx";
    public static final String DEAD_LETTER_QUEUE = "game.user-deleted.v1.dlq";

    @Bean
    Declarables gameUserDeletedTopology(@Value("${game.messaging.retry-delay-ms:30000}") long retryDelayMs) {
        TopicExchange events = new TopicExchange(UserMessaging.EXCHANGE, true, false);
        DirectExchange retry = new DirectExchange(RETRY_EXCHANGE, true, false);
        DirectExchange deadLetter = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue main = QueueBuilder.durable(QUEUE)
                .deadLetterExchange(RETRY_EXCHANGE).deadLetterRoutingKey(RETRY_QUEUE).build();
        Queue retryQueue = QueueBuilder.durable(RETRY_QUEUE).ttl((int) retryDelayMs)
                .deadLetterExchange("").deadLetterRoutingKey(QUEUE).build();
        Queue parked = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        Binding mainBinding = BindingBuilder.bind(main).to(events).with(UserMessaging.USER_DELETED_ROUTING_KEY);
        Binding retryBinding = BindingBuilder.bind(retryQueue).to(retry).with(RETRY_QUEUE);
        Binding parkedBinding = BindingBuilder.bind(parked).to(deadLetter).with(DEAD_LETTER_QUEUE);
        return new Declarables(events, retry, deadLetter, main, retryQueue, parked, mainBinding, retryBinding,
                parkedBinding);
    }
}

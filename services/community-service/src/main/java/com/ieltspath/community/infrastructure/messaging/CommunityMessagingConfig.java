package com.ieltspath.community.infrastructure.messaging;

import com.ieltspath.events.community.CommunityMessaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunityMessagingConfig {

    @Bean
    TopicExchange communityEventsExchange(@Value("${community.messaging.exchange:" + CommunityMessaging.EXCHANGE + "}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    RabbitTemplate communityRabbitTemplate(CachingConnectionFactory connectionFactory) {
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        return template;
    }
}

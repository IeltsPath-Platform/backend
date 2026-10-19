package com.ieltspath.notification.infrastructure;

import com.ieltspath.events.community.CommunityEventTypes;

import com.ieltspath.events.community.CommunityMessaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

@Configuration
@EnableWebSocketMessageBroker
public class NotificationMessagingConfig implements WebSocketMessageBrokerConfigurer {
    private final Environment environment;
    public NotificationMessagingConfig(Environment environment){this.environment=environment;}
    @Bean TopicExchange communityExchange(){return new TopicExchange(CommunityMessaging.EXCHANGE,true,false);}
    @Bean DirectExchange retryExchange(){return new DirectExchange("community.events.retry",true,false);}
    @Bean DirectExchange deadLetterExchange(){return new DirectExchange("community.events.dlx",true,false);}
    @Bean Queue notificationQueue(){return QueueBuilder.durable("community.notifications.v1").build();}
    @Bean Queue notificationDlq(){return QueueBuilder.durable("community.notifications.v1.dlq").build();}
    @Bean Binding notificationBinding(){return BindingBuilder.bind(notificationQueue()).to(communityExchange()).with("community.#");}
    @Bean Binding notificationDlqBinding(){return BindingBuilder.bind(notificationDlq()).to(deadLetterExchange()).with("community.notifications.v1.dlq");}
    @Bean Declarables retryQueues(){
        var routes=CommunityEventTypes.ORDERED;
        var declarations=new java.util.ArrayList<Declarable>();
        for(String route:routes){
            String retryRoute="retry."+route;
            Queue queue=QueueBuilder.durable("community.notifications.retry."+route)
                    .ttl(5000).deadLetterExchange(CommunityMessaging.EXCHANGE).deadLetterRoutingKey(route).build();
            declarations.add(queue);
            declarations.add(BindingBuilder.bind(queue).to(retryExchange()).with(retryRoute));
        }
        return new Declarables(declarations);
    }
    @Bean SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory){
        var factory=new SimpleRabbitListenerContainerFactory(); factory.setConnectionFactory(connectionFactory); factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); factory.setDefaultRequeueRejected(true); factory.setPrefetchCount(20); return factory;
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry){registry.addEndpoint("/ws/notifications").setAllowedOriginPatterns(environment.getProperty("app.frontend-origin","http://localhost:5173"));}
    @Override public void configureMessageBroker(MessageBrokerRegistry registry){registry.enableSimpleBroker("/queue");registry.setUserDestinationPrefix("/user");registry.setApplicationDestinationPrefixes("/app");}
    @Override public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration){
        registration.interceptors(new ChannelInterceptor(){
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel){
                StompHeaderAccessor accessor=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
                if(accessor!=null && accessor.getCommand()!=null){
                    if(accessor.getCommand()==StompCommand.SEND) throw new IllegalArgumentException("Client messages are not accepted");
                    if(accessor.getCommand()==StompCommand.SUBSCRIBE && !"/user/queue/notifications".equals(accessor.getDestination())) throw new IllegalArgumentException("Subscription is not allowed");
                }
                return message;
            }
        });
    }
}

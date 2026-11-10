package com.ieltspath.user.infrastructure.messaging;

import com.ieltspath.events.user.PasswordResetRequestedTopology;
import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserMessagingConfig {
    @Bean
    Declarables passwordResetRequestedTopology() { return PasswordResetRequestedTopology.declarables(); }
}

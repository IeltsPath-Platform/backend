package com.ieltspath.notification.infrastructure.push;

import com.ieltspath.notification.application.port.PushSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FcmPushConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "notification.push.worker-enabled", havingValue = "true")
    PushSender fcmPushSender(@Value("${notification.push.firebase.project-id:}") String projectId,
                             @Value("${notification.push.firebase.android-channel-id:default}") String channelId) {
        return new FcmPushSender(projectId, channelId);
    }

    @Bean
    @ConditionalOnProperty(name = "notification.push.worker-enabled", havingValue = "false", matchIfMissing = true)
    PushSender disabledPushSender() {
        return new PushSender() {
            @Override public boolean isReady() { return false; }
            @Override public String send(com.ieltspath.notification.domain.vo.PushSendSnapshot snapshot) {
                throw new com.ieltspath.notification.application.port.PushSendException(
                        com.ieltspath.notification.application.port.PushSendException.Category.CONFIGURATION,
                        "WORKER_DISABLED");
            }
        };
    }
}

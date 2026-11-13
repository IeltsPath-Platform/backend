package com.ieltspath.notification.infrastructure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.notification.application.DispatchPushDeliveriesUseCase;
import com.ieltspath.notification.domain.repository.PushDeliveryRepository;
import com.ieltspath.notification.domain.repository.PushDeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.Clock;

/**
 * Push sending exists only when {@code notification.fcm.enabled=true}. Credentials come from Application Default
 * Credentials ({@code GOOGLE_APPLICATION_CREDENTIALS} pointing at a service-account file outside the repo, or the
 * runtime's workload identity); nothing is read from config-repo. Without it, devices still register and deliveries
 * stay PENDING.
 */
@Configuration
@ConditionalOnProperty(name = "notification.fcm.enabled", havingValue = "true")
public class FcmPushConfig {

    @Bean
    FirebaseMessaging firebaseMessaging(@Value("${notification.fcm.project-id}") String projectId) throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(projectId)
                .build();
        return FirebaseMessaging.getInstance(FirebaseApp.initializeApp(options, "notification-service"));
    }

    @Bean
    DispatchPushDeliveriesUseCase dispatchPushDeliveries(PushDeliveryRepository deliveries, PushDeviceRepository devices,
                                                        FirebaseMessaging messaging, ResilientCall resilience) {
        return new DispatchPushDeliveriesUseCase(deliveries, devices, new FcmPushSender(messaging, resilience),
                Clock.systemUTC());
    }

    @Bean
    PushDeliveryJob pushDeliveryJob(DispatchPushDeliveriesUseCase dispatch) { return new PushDeliveryJob(dispatch); }

    // shortcut: SENT/FAILED deliveries and devices idle for months are never pruned; add a daily cleanup job once push
    // volume or stale tokens show up in metrics.
    /** Safe on many instances: each run claims its own rows with SKIP LOCKED. */
    public record PushDeliveryJob(DispatchPushDeliveriesUseCase dispatch) {
        @Scheduled(fixedDelayString = "${notification.fcm.dispatch-delay-ms:5000}")
        public void run() {
            while (dispatch.dispatchDue() == DispatchPushDeliveriesUseCase.BATCH) {
                // A full batch means more are due; keep draining before the next tick.
            }
        }
    }
}

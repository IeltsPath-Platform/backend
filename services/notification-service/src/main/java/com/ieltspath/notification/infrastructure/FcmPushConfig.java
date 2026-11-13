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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Push sending exists only when {@code notification.fcm.enabled=true}. Credentials come from the service-account file
 * named by {@code GOOGLE_APPLICATION_CREDENTIALS} (outside the repo), or Application Default Credentials when unset;
 * the key itself is never in config-repo. Without it, devices still register and deliveries
 * stay PENDING.
 */
@Configuration
@ConditionalOnProperty(name = "notification.fcm.enabled", havingValue = "true")
public class FcmPushConfig {

    @Bean
    FirebaseMessaging firebaseMessaging(@Value("${notification.fcm.project-id:}") String projectId,
                                        @Value("${notification.fcm.credentials-file:}") String credentialsFile)
            throws IOException {
        // Read the path through Spring: a value from the root .env is a property, not an OS env var, so ADC alone misses it.
        GoogleCredentials credentials;
        if (credentialsFile.isBlank()) {
            credentials = GoogleCredentials.getApplicationDefault();
        } else {
            try (InputStream in = Files.newInputStream(Path.of(credentialsFile))) {
                credentials = GoogleCredentials.fromStream(in);
            }
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId.isBlank() ? null : projectId) // null: SDK takes project_id from the credentials
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

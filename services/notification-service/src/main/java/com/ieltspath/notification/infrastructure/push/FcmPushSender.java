package com.ieltspath.notification.infrastructure.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.ieltspath.notification.application.port.PushSendException;
import com.ieltspath.notification.application.port.PushTransientException;
import com.ieltspath.notification.application.port.PushSender;
import com.ieltspath.notification.domain.vo.Notification;
import com.ieltspath.notification.domain.vo.PushSendSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FcmPushSender implements PushSender, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);
    private static final long MAX_RETRY_AFTER_SECONDS = 24 * 60 * 60;
    private final String projectId;
    private final String channelId;
    private final AtomicBoolean attempted = new AtomicBoolean();
    private volatile FirebaseApp app;
    private volatile boolean unavailable;

    public FcmPushSender(String projectId, String channelId) {
        this.projectId = projectId;
        this.channelId = channelId;
    }

    @Override
    public boolean isReady() {
        initialize();
        return app != null;
    }

    @Override
    public String send(PushSendSnapshot snapshot) {
        if (!isReady()) throw new PushSendException(PushSendException.Category.CONFIGURATION, "CREDENTIALS_UNAVAILABLE");
        Instant expires = snapshot.job().expiresAt();
        long ttlMillis = Math.max(0, Duration.between(Instant.now(), expires).toMillis());
        if (ttlMillis == 0) throw new PushSendException(PushSendException.Category.PERMANENT, "EXPIRED");
        Map<String, String> data = payload(snapshot.notification());
        Message message = Message.builder().setToken(snapshot.token()).putAllData(data)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle("IELTSPath notification").setBody(bodyFor(snapshot.notification().eventType())).build())
                .setAndroidConfig(AndroidConfig.builder().setTtl(ttlMillis)
                        .setNotification(AndroidNotification.builder().setChannelId(channelId).build()).build())
                .build();
        try {
            return FirebaseMessaging.getInstance(app).send(message);
        } catch (FirebaseMessagingException failure) {
            MessagingErrorCode code = failure.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED)
                throw new PushSendException(PushSendException.Category.UNREGISTERED, "UNREGISTERED");
            if (code == MessagingErrorCode.UNAVAILABLE || code == MessagingErrorCode.INTERNAL
                    || code == MessagingErrorCode.QUOTA_EXCEEDED)
                throw new PushTransientException(code.name(), retryAfterSeconds(failure));
            if (failure.getErrorCode() == com.google.firebase.ErrorCode.PERMISSION_DENIED
                    || failure.getErrorCode() == com.google.firebase.ErrorCode.UNAUTHENTICATED
                    || code == MessagingErrorCode.SENDER_ID_MISMATCH
                    || code == MessagingErrorCode.THIRD_PARTY_AUTH_ERROR) {
                throw new PushSendException(PushSendException.Category.CONFIGURATION, code == null
                        ? failure.getErrorCode().name() : code.name());
            }
            throw new PushSendException(PushSendException.Category.PERMANENT,
                    code == null ? "PROVIDER_REJECTED" : code.name());
        }
    }

    private synchronized void initialize() {
        if (app != null || unavailable || !attempted.compareAndSet(false, true)) return;
        try {
            FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault())
                    .setConnectTimeout(3_000).setReadTimeout(10_000).setWriteTimeout(10_000);
            if (projectId != null && !projectId.isBlank()) options.setProjectId(projectId);
            app = FirebaseApp.initializeApp(options.build(), "ieltspath-notification");
        } catch (Exception failure) {
            unavailable = true;
            log.error("FCM initialization unavailable: errorType={}", failure.getClass().getSimpleName());
        }
    }

    private Map<String, String> payload(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", notification.id().toString());
        data.put("eventType", notification.eventType());
        if (notification.actorId() != null) data.put("actorId", notification.actorId().toString());
        if (notification.postId() != null) data.put("postId", notification.postId().toString());
        if (notification.commentId() != null) data.put("commentId", notification.commentId().toString());
        if (notification.gradingJobId() != null) data.put("gradingJobId", notification.gradingJobId().toString());
        return data;
    }

    private long retryAfterSeconds(FirebaseMessagingException failure) {
        try {
            Object header = failure.getHttpResponse().getHeaders().get("Retry-After");
            if (header == null) return 0;
            String value = header instanceof Iterable<?> values
                    ? values.iterator().hasNext() ? values.iterator().next().toString() : ""
                    : header.toString();
            try {
                return clampRetryAfter(Long.parseLong(value));
            } catch (NumberFormatException notSeconds) {
                Instant retryAt = java.time.ZonedDateTime.parse(value, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant();
                return clampRetryAfter(Duration.between(Instant.now(), retryAt).toSeconds());
            }
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private long clampRetryAfter(long seconds) {
        return Math.max(0, Math.min(MAX_RETRY_AFTER_SECONDS, seconds));
    }

    private String bodyFor(String eventType) {
        if (eventType.contains("grading")) return "Your IELTS grading update is ready.";
        if (eventType.contains("subscription")) return "Your IELTSPath subscription has been updated.";
        return "There is new activity in your IELTSPath account.";
    }

    @Override
    public synchronized void close() {
        if (app != null) app.delete();
    }
}

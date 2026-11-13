package com.ieltspath.notification.infrastructure;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import com.ieltspath.notification.application.PushSender;

import java.util.Map;

/**
 * Sends through the Firebase Admin SDK (FCM HTTP v1). Sent as {@code WRITE}, so the call is never repeated here; the
 * delivery job owns the retry. Error codes follow https://firebase.google.com/docs/cloud-messaging/manage-tokens.
 */
public class FcmPushSender implements PushSender {
    static final String TARGET = "fcm";
    private final FirebaseMessaging messaging;
    private final ResilientCall resilience;

    public FcmPushSender(FirebaseMessaging messaging, ResilientCall resilience) {
        this.messaging = messaging;
        this.resilience = resilience;
    }

    @Override
    public String send(String token, String title, String body, Map<String, String> data) {
        Message message = Message.builder().setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data).build();
        try {
            return resilience.call(TARGET, CallKind.WRITE, () -> {
                try {
                    return messaging.send(message);
                } catch (FirebaseMessagingException rejected) {
                    throw new PushSendException(classify(rejected.getMessagingErrorCode()), code(rejected));
                }
            });
        } catch (PushSendException rejected) {
            throw rejected;
        } catch (DownstreamUnavailableException unavailable) {
            throw new PushSendException(Failure.TRANSIENT, "FCM_UNAVAILABLE");
        } catch (RuntimeException unexpected) {
            throw new PushSendException(Failure.TRANSIENT, unexpected.getClass().getSimpleName());
        }
    }

    static Failure classify(MessagingErrorCode code) {
        if (code == null) return Failure.TRANSIENT;
        return switch (code) {
            case UNREGISTERED, SENDER_ID_MISMATCH -> Failure.TOKEN_INVALID;
            // The payload is fixed text, so an invalid argument points at the token or at the APNs setup, not the body.
            case INVALID_ARGUMENT, THIRD_PARTY_AUTH_ERROR -> Failure.PERMANENT;
            default -> Failure.TRANSIENT;
        };
    }

    private static String code(FirebaseMessagingException rejected) {
        return rejected.getMessagingErrorCode() != null ? rejected.getMessagingErrorCode().name()
                : String.valueOf(rejected.getErrorCode());
    }
}

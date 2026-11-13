package com.ieltspath.notification.infrastructure;

import com.google.firebase.messaging.MessagingErrorCode;
import com.ieltspath.notification.application.PushSender.Failure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FcmPushSenderTest {
    @Test
    void fcmErrorCodesDecideWhetherToRetryOrDropTheDevice() {
        assertEquals(Failure.TOKEN_INVALID, FcmPushSender.classify(MessagingErrorCode.UNREGISTERED));
        assertEquals(Failure.TOKEN_INVALID, FcmPushSender.classify(MessagingErrorCode.SENDER_ID_MISMATCH));
        assertEquals(Failure.PERMANENT, FcmPushSender.classify(MessagingErrorCode.INVALID_ARGUMENT));
        assertEquals(Failure.TRANSIENT, FcmPushSender.classify(MessagingErrorCode.QUOTA_EXCEEDED));
        assertEquals(Failure.TRANSIENT, FcmPushSender.classify(MessagingErrorCode.UNAVAILABLE));
        assertEquals(Failure.TRANSIENT, FcmPushSender.classify(null));
    }
}

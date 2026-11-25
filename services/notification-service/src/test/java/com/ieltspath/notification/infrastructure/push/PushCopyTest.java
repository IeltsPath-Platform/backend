package com.ieltspath.notification.infrastructure.push;

import com.ieltspath.notification.application.NotificationEventTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushCopyTest {
    @Test
    void everyStoredEventTypeHasItsOwnWording() {
        PushCopy fallback = PushCopy.forEventType(null);

        assertThat(NotificationEventTypes.ORDERED)
                .allSatisfy(eventType -> assertThat(PushCopy.forEventType(eventType)).isNotEqualTo(fallback));
    }

    @Test
    void nullAndUnknownEventTypesUseFallbackCopy() {
        PushCopy fallback = PushCopy.forEventType(null);

        assertThat(PushCopy.forEventType("unknown.event.v1")).isEqualTo(fallback);
        assertThat(fallback.title()).isEqualTo("IELTSPath");
        assertThat(fallback.body()).isEqualTo("Bạn có thông báo mới trên IELTSPath.");
    }
}

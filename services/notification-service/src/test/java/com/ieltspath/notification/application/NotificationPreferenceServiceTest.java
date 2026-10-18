package com.ieltspath.notification.application;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationPreferenceServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final NotificationPreferenceService service=new NotificationPreferenceService(jdbc);

    @Test void savesPreferenceForOneSupportedEventType() {
        UUID owner=UUID.randomUUID();
        service.update(owner,NotificationEventTypes.COMMENT_MENTIONED,false);
        verify(jdbc).update(contains("INSERT INTO notification_preferences"),eq(owner),eq(NotificationEventTypes.COMMENT_MENTIONED),eq(false));
    }

    @Test void rejectsUnknownEventTypeWithoutWriting() {
        assertThrows(IllegalArgumentException.class,()->service.update(UUID.randomUUID(),"community.unknown.v1",false));
        verifyNoInteractions(jdbc);
    }
}

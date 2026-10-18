package com.ieltspath.notification.application;

import com.ieltspath.events.community.CommunityEventTypes;

import com.ieltspath.notification.domain.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NotificationPreferenceServiceTest {
    private final NotificationPreferenceRepository repository=mock(NotificationPreferenceRepository.class);
    private final NotificationPreferenceService service=new NotificationPreferenceService(repository);

    @Test void savesPreferenceForOneSupportedEventType() {
        UUID owner=UUID.randomUUID();
        service.update(owner,CommunityEventTypes.COMMENT_MENTIONED,false);
        verify(repository).save(owner,CommunityEventTypes.COMMENT_MENTIONED,false);
    }

    @Test void rejectsUnknownEventTypeWithoutWriting() {
        assertThrows(IllegalArgumentException.class,()->service.update(UUID.randomUUID(),"community.unknown.v1",false));
        verifyNoInteractions(repository);
    }
}

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

    @Test void includesStoredAssessmentAndAccessNoticesByDefault() {
        when(repository.findAll(any())).thenReturn(java.util.Map.of());
        var result = service.list(UUID.randomUUID());
        org.junit.jupiter.api.Assertions.assertEquals(NotificationEventTypes.ORDERED.size(), result.size());
        org.junit.jupiter.api.Assertions.assertTrue(result.stream().allMatch(NotificationPreference::enabled));
        org.junit.jupiter.api.Assertions.assertTrue(NotificationEventTypes.ALL.contains("assessment.grading-requested.v1"));
        org.junit.jupiter.api.Assertions.assertTrue(NotificationEventTypes.ALL.contains("access.subscription-granted.v1"));
    }
}

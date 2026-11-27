package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteAvatarUseCaseTest {
    @Mock AvatarStoragePort storage;
    @Mock AvatarReferenceWriter writer;

    private DeleteAvatarUseCase useCase() {
        DeleteAvatarUseCase useCase = new DeleteAvatarUseCase(storage, writer);
        ReflectionTestUtils.setField(useCase, "bucket", "avatars");
        ReflectionTestUtils.setField(useCase, "projectUrl", "https://project.supabase.co");
        return useCase;
    }

    @Test void clearsAndDeletesOwnedObject() {
        UUID userId = UUID.randomUUID();
        String key = "images/" + userId + "/11111111-1111-1111-1111-111111111111.webp";
        when(writer.replace(userId, null, null)).thenReturn(new AvatarReferenceWriter.Outcome(profile(userId),
                "https://project.supabase.co/storage/v1/object/public/avatars/" + key, false));
        assertNull(useCase().execute(userId).avatarReference());
        verify(storage).delete(key);
    }

    @Test void isIdempotentForMissingAvatar() {
        UUID userId = UUID.randomUUID();
        when(writer.replace(userId, null, null)).thenReturn(new AvatarReferenceWriter.Outcome(profile(userId), null, false));
        assertNull(useCase().execute(userId).avatarReference());
        verifyNoInteractions(storage);
    }

    @Test void refusesADeletedAccount() {
        UUID userId = UUID.randomUUID();
        when(writer.replace(userId, null, null)).thenReturn(new AvatarReferenceWriter.Outcome(null, null, true));
        assertThrows(UserManagementRuleViolationException.class, () -> useCase().execute(userId));
        verifyNoInteractions(storage);
    }

    private static LearnerProfile profile(UUID userId) {
        return LearnerProfile.builder().userId(userId).displayName("Name")
                .timezone("UTC").visibility(ProfileVisibility.PUBLIC).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();
    }
}

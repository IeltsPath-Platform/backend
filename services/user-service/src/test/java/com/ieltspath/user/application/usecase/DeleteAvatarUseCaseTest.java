package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeleteAvatarUseCaseTest {
    @Mock AvatarStoragePort storage;
    @Mock LearnerProfileRepository profiles;
    @Mock UserRepository users;

    @Test void clearsAndDeletesOwnedObject() {
        UUID userId = UUID.randomUUID();
        String key = "images/" + userId + "/11111111-1111-1111-1111-111111111111.webp";
        LearnerProfile profile = LearnerProfile.builder().userId(userId).displayName("Name")
                .avatarReference("https://project.supabase.co/storage/v1/object/public/avatars/" + key)
                .timezone("UTC").visibility(ProfileVisibility.PUBLIC).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DeleteAvatarUseCase useCase = new DeleteAvatarUseCase(storage, profiles, users);
        ReflectionTestUtils.setField(useCase, "bucket", "avatars");
        ReflectionTestUtils.setField(useCase, "projectUrl", "https://project.supabase.co");
        assertNull(useCase.execute(userId).avatarReference());
        verify(storage).delete(key);
    }

    @Test void isIdempotentForMissingAvatar() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = LearnerProfile.builder().userId(userId).displayName("Name")
                .timezone("UTC").visibility(ProfileVisibility.PUBLIC).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DeleteAvatarUseCase useCase = new DeleteAvatarUseCase(storage, profiles, users);
        assertNull(useCase.execute(userId).avatarReference());
        verifyNoInteractions(storage);
    }
}

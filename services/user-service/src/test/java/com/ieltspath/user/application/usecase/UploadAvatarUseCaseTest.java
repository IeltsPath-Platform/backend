package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.application.port.AvatarUploadRateLimiter;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadAvatarUseCaseTest {
    @Mock AvatarStoragePort storage;
    @Mock AvatarUploadRateLimiter rateLimiter;
    @Mock LearnerProfileRepository profiles;
    @Mock UserRepository users;
    private UploadAvatarUseCase useCase;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach void setUp() {
        useCase = new UploadAvatarUseCase(storage, rateLimiter, profiles, users);
        ReflectionTestUtils.setField(useCase, "bucket", "avatars");
        ReflectionTestUtils.setField(useCase, "projectUrl", "https://project.supabase.co");
    }

    @Test void storesPngAndDeletesPreviousOwnedAvatar() {
        String oldKey = "images/" + userId + "/11111111-1111-1111-1111-111111111111.png";
        LearnerProfile old = profile("https://project.supabase.co/storage/v1/object/public/avatars/" + oldKey);
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(old));
        when(storage.put(anyString(), any(), anyString())).thenAnswer(inv ->
                "https://project.supabase.co/storage/v1/object/public/avatars/" + inv.getArgument(0));
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(userId, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}, "image/png");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storage).put(key.capture(), any(), eq("image/png"));
        assertTrue(key.getValue().matches("images/" + userId + "/[0-9a-f-]{36}\\.png"));
        verify(storage).delete(oldKey);
    }

    @Test void rollsBackNewObjectWhenProfileSaveFails() {
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile(null)));
        when(storage.put(anyString(), any(), anyString())).thenAnswer(inv ->
                "https://project.supabase.co/storage/v1/object/public/avatars/" + inv.getArgument(0));
        when(profiles.save(any())).thenThrow(new IllegalStateException("db failure"));
        assertThrows(IllegalStateException.class, () -> useCase.execute(userId,
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}, "image/png"));
        verify(storage).delete(argThat(k -> k.startsWith("images/" + userId + "/")));
    }

    @Test void rejectsMimeSpoofBeforeStorage() {
        assertThrows(com.ieltspath.commonstorage.UploadRejectedException.class,
                () -> useCase.execute(userId, "not a png".getBytes(), "image/png"));
        verifyNoInteractions(storage);
    }

    @Test void neverDeletesExternalPreviousAvatar() {
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile("https://evil.example/avatar.png")));
        when(storage.put(anyString(), any(), anyString())).thenReturn("https://project.supabase.co/avatar");
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
        useCase.execute(userId, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}, "image/jpeg");
        verify(storage, never()).delete(anyString());
    }

    private LearnerProfile profile(String avatar) {
        return LearnerProfile.builder().userId(userId).displayName("Name").avatarReference(avatar)
                .timezone("UTC").visibility(ProfileVisibility.PUBLIC).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();
    }
}

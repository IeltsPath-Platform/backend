package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.AccountDeletionCleanupPort;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvatarReferenceWriterTest {
    @Mock UserLock userLock;
    @Mock UserRepository users;
    @Mock LearnerProfileRepository profiles;
    @Mock AccountDeletionCleanupPort cleanupPort;
    @InjectMocks AvatarReferenceWriter writer;
    private final UUID userId = UUID.randomUUID();

    @Test
    void readsTheProfileOnlyAfterTakingTheAccountLock() {
        when(users.findById(userId)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        LearnerProfile profile = LearnerProfile.builder().userId(userId).displayName("Name").avatarReference("old")
                .timezone("UTC").visibility(ProfileVisibility.PUBLIC).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var outcome = writer.replace(userId, "new", "images/" + userId + "/new.png");

        var order = inOrder(userLock, users, profiles);
        order.verify(userLock).lock(userId);
        order.verify(users).findById(userId);
        order.verify(profiles).findByUserId(userId);
        assertThat(outcome.previousReference()).isEqualTo("old");
        assertThat(outcome.profile().getAvatarReference()).isEqualTo("new");
        verifyNoInteractions(cleanupPort);
    }

    @Test
    void queuesTheJustUploadedObjectWhenTheAccountWasDeletedMeanwhile() {
        when(users.findById(userId)).thenReturn(Optional.of(user(UserStatus.DELETED)));
        String key = "images/" + userId + "/new.png";

        var outcome = writer.replace(userId, "https://project/new.png", key);

        assertThat(outcome.accountDeleted()).isTrue();
        verify(cleanupPort).queueAvatarDeletion(userId, key);
        verify(profiles, never()).save(any());
    }

    private User user(UserStatus status) {
        return User.builder().id(userId).email(new Email("a@example.com")).fullName("A").status(status).build();
    }
}

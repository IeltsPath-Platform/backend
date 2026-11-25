package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.GoogleAccountUnlinkedV1;
import com.ieltspath.user.application.exception.ReauthRequiredException;
import com.ieltspath.user.application.port.ReauthTokenStore;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.OAuthIdentityNotFoundException;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnlinkOAuthIdentityUseCaseTest {
    private final OAuthIdentityRepository identities = mock(OAuthIdentityRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ReauthTokenStore tokens = mock(ReauthTokenStore.class);
    private final UserNoticeOutbox notices = mock(UserNoticeOutbox.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private UnlinkOAuthIdentityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UnlinkOAuthIdentityUseCase(identities, users, tokens, notices);
        when(identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE)).thenReturn(Optional.of(
                OAuthIdentity.builder().userId(userId).provider(OAuthProvider.GOOGLE).providerSubject("sub").build()));
        when(users.findById(userId)).thenReturn(Optional.of(User.builder().id(userId)
                .email(new Email("person@example.test")).fullName("Person").passwordHash("hash")
                .status(UserStatus.ACTIVE).build()));
    }

    @Test
    void preservesTheExistingUnlinkBehaviorWhenTheFeatureFlagIsOffAndSendsNotice() {
        useCase.execute(userId, "google");

        verify(identities).deleteByUserIdAndProvider(userId, OAuthProvider.GOOGLE);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(notices).enqueue(any(UUID.class), eq(userId), eq(GoogleAccountUnlinkedV1.EVENT_TYPE), event.capture());
        assertThat(event.getValue()).isInstanceOf(GoogleAccountUnlinkedV1.class);
    }

    @Test
    void requiresAValidUserAndSessionBoundTokenWhenTheFeatureFlagIsOn() {
        ReflectionTestUtils.setField(useCase, "reauthRequired", true);

        assertThatThrownBy(() -> useCase.execute(userId, "google", null, sessionId))
                .isInstanceOf(ReauthRequiredException.class)
                .hasMessage("Vui lòng xác thực lại để thực hiện thao tác này.");
        verify(identities, never()).deleteByUserIdAndProvider(any(), any());
    }

    @Test
    void consumesMatchingReauthenticationBeforeRemovingGoogle() {
        ReflectionTestUtils.setField(useCase, "reauthRequired", true);
        when(tokens.consume("token")).thenReturn(Optional.of(
                new ReauthTokenStore.Grant(userId, "PASSWORD", sessionId)));

        useCase.execute(userId, "google", "token", sessionId);

        verify(tokens).consume("token");
        verify(identities).deleteByUserIdAndProvider(userId, OAuthProvider.GOOGLE);
    }

    @Test
    void rejectsAReauthenticationGrantForAnotherUserOrSession() {
        ReflectionTestUtils.setField(useCase, "reauthRequired", true);
        when(tokens.consume("token")).thenReturn(Optional.of(
                new ReauthTokenStore.Grant(UUID.randomUUID(), "PASSWORD", sessionId)));
        assertThatThrownBy(() -> useCase.execute(userId, "google", "token", sessionId))
                .isInstanceOf(ReauthRequiredException.class);
        when(tokens.consume("token")).thenReturn(Optional.of(
                new ReauthTokenStore.Grant(userId, "PASSWORD", UUID.randomUUID())));
        assertThatThrownBy(() -> useCase.execute(userId, "google", "token", sessionId))
                .isInstanceOf(ReauthRequiredException.class);
        verify(identities, never()).deleteByUserIdAndProvider(any(), any());
    }

    @Test
    void keepsTheLastIdentityWhenNoPasswordHasBeenSet() {
        when(users.findById(userId)).thenReturn(Optional.of(User.builder().id(userId)
                .email(new Email("person@example.test")).fullName("Person").status(UserStatus.ACTIVE).build()));
        when(identities.findByUserId(userId)).thenReturn(List.of(OAuthIdentity.builder().build()));

        assertThatThrownBy(() -> useCase.execute(userId, "google"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không thể hủy liên kết");
        verify(identities, never()).deleteByUserIdAndProvider(any(), any());
    }
}

package com.group01.user.application.usecase;

import com.group01.user.application.result.OAuthIdentityResult;
import com.group01.user.domain.aggregate.OAuthIdentity;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.OAuthIdentityNotFoundException;
import com.group01.user.domain.repository.OAuthIdentityRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthIdentityUseCaseTest {
    @Mock private OAuthIdentityRepository oauthIdentityRepository;
    @Mock private UserRepository userRepository;

    private GetOAuthIdentitiesUseCase getOAuthIdentitiesUseCase;
    private UnlinkOAuthIdentityUseCase unlinkOAuthIdentityUseCase;

    @BeforeEach
    void setUp() {
        getOAuthIdentitiesUseCase = new GetOAuthIdentitiesUseCase(oauthIdentityRepository);
        unlinkOAuthIdentityUseCase = new UnlinkOAuthIdentityUseCase(oauthIdentityRepository, userRepository);
    }

    @Test
    void getOAuthIdentitiesReturnsList() {
        UUID userId = UUID.randomUUID();
        OAuthIdentity identity = OAuthIdentity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(OAuthProvider.GOOGLE)
                .providerSubject("google-sub")
                .linkedAt(LocalDateTime.now())
                .build();

        when(oauthIdentityRepository.findByUserId(userId)).thenReturn(List.of(identity));

        List<OAuthIdentityResult> results = getOAuthIdentitiesUseCase.execute(userId);
        assertEquals(1, results.size());
        assertEquals("GOOGLE", results.get(0).provider());
    }

    @Test
    void unlinkOAuthIdentitySucceedsWhenUserHasPassword() {
        UUID userId = UUID.randomUUID();
        OAuthIdentity identity = OAuthIdentity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(OAuthProvider.GOOGLE)
                .build();

        User user = User.builder()
                .id(userId)
                .passwordHash("hashed-password")
                .build();

        when(oauthIdentityRepository.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE))
                .thenReturn(Optional.of(identity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        unlinkOAuthIdentityUseCase.execute(userId, "GOOGLE");
        verify(oauthIdentityRepository).deleteByUserIdAndProvider(userId, OAuthProvider.GOOGLE);
    }

    @Test
    void unlinkOAuthIdentityThrowsWhenNoPasswordAndSingleIdentity() {
        UUID userId = UUID.randomUUID();
        OAuthIdentity identity = OAuthIdentity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(OAuthProvider.GOOGLE)
                .build();

        User user = User.builder()
                .id(userId)
                .passwordHash(null)
                .build();

        when(oauthIdentityRepository.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE))
                .thenReturn(Optional.of(identity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(oauthIdentityRepository.findByUserId(userId)).thenReturn(List.of(identity));

        assertThrows(IllegalStateException.class, () -> unlinkOAuthIdentityUseCase.execute(userId, "GOOGLE"));
    }

    @Test
    void unlinkOAuthIdentityThrowsWhenNotFound() {
        UUID userId = UUID.randomUUID();
        when(oauthIdentityRepository.findByUserIdAndProvider(userId, OAuthProvider.FACEBOOK))
                .thenReturn(Optional.empty());

        assertThrows(OAuthIdentityNotFoundException.class, () -> unlinkOAuthIdentityUseCase.execute(userId, "FACEBOOK"));
    }
}


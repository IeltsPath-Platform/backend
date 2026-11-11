package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.exception.GoogleAccountNotActiveException;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.application.result.GoogleSignInResult;
import com.ieltspath.user.domain.exception.EmailAlreadyExistsException;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleSignInUseCaseTest {
    @Mock GoogleIdTokenVerifier verifier;
    @Mock LinkGoogleAccountUseCase linker;
    @Mock LoginHistoryRecorder history;

    private GoogleSignInUseCase useCase;
    private final GoogleIdentity identity = new GoogleIdentity("subject", "person@gmail.com", "Person", null);
    private final LoginContext context = new LoginContext("127.0.0.1", "test-agent");
    private final GoogleSignInResult result = new GoogleSignInResult(UUID.randomUUID(),
            new AuthTokenResult("access", "refresh", 3600, 604800));

    @BeforeEach
    void setUp() {
        useCase = new GoogleSignInUseCase(verifier, linker, history);
        when(verifier.verify("signed-token")).thenReturn(identity);
    }

    @Test
    void retriesOnceAfterEmailUniquenessRaceAndRecordsSuccess() {
        doThrow(new EmailAlreadyExistsException("Email already exists"))
                .doReturn(result).when(linker).execute(identity);

        assertThat(useCase.execute("signed-token", context)).isEqualTo(result);

        verify(linker, org.mockito.Mockito.times(2)).execute(identity);
        verify(history).recordSuccess(result.userId(), context);
    }

    @Test
    void recordsInactiveAccountFailureAgainstTheKnownUser() {
        GoogleAccountNotActiveException inactive = new GoogleAccountNotActiveException(result.userId(), UserStatus.LOCKED);
        when(linker.execute(identity)).thenThrow(inactive);

        assertThatThrownBy(() -> useCase.execute("signed-token", context)).isSameAs(inactive);

        verify(history).recordFailure(result.userId(), LoginFailureReason.ACCOUNT_LOCKED, context);
    }

    @Test
    void failedTokenVerificationDoesNotReachAccountOrHistory() {
        when(verifier.verify("signed-token")).thenThrow(new RuntimeException("invalid"));

        assertThatThrownBy(() -> useCase.execute("signed-token", context)).isInstanceOf(RuntimeException.class);

        verify(linker, never()).execute(identity);
        verify(history, never()).recordSuccess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(history, never()).recordFailure(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginHistoryFailureDoesNotUndoSuccessfulSignIn() {
        when(linker.execute(identity)).thenReturn(result);
        doThrow(new IllegalStateException("audit storage unavailable"))
                .when(history).recordSuccess(result.userId(), context);

        assertThat(useCase.execute("signed-token", context)).isEqualTo(result);
    }
}

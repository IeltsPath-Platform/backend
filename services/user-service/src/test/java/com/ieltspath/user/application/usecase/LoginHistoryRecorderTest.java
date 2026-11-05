package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.domain.aggregate.LoginAttempt;
import com.ieltspath.user.domain.repository.LoginHistoryRepository;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginHistoryRecorderTest {
    @Mock
    private LoginHistoryRepository repository;
    @InjectMocks
    private LoginHistoryRecorder recorder;

    @Test
    void savesSuccessAndFailureWithTheirContext() {
        UUID userId = UUID.randomUUID();
        LoginContext context = new LoginContext("203.0.113.9", "Mozilla/5.0");

        recorder.recordSuccess(userId, context);
        recorder.recordFailure(userId, LoginFailureReason.ACCOUNT_LOCKED, context);

        ArgumentCaptor<LoginAttempt> saved = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(LoginAttempt::isSuccess).containsExactly(true, false);
        assertThat(saved.getAllValues().get(1).getFailureReason()).isEqualTo(LoginFailureReason.ACCOUNT_LOCKED);
        assertThat(saved.getAllValues()).allSatisfy(attempt -> {
            assertThat(attempt.getUserId()).isEqualTo(userId);
            assertThat(attempt.getIpAddress()).isEqualTo("203.0.113.9");
            assertThat(attempt.getUserAgent()).isEqualTo("Mozilla/5.0");
        });
    }

    @Test
    void aFailedWriteNeverFailsTheSignIn() {
        when(repository.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> recorder.recordSuccess(UUID.randomUUID(), LoginContext.UNKNOWN)).doesNotThrowAnyException();
    }

    @Test
    void contextIsCutToColumnSizes() {
        LoginContext context = new LoginContext("1".repeat(80), "a".repeat(400));

        assertThat(context.ipAddress()).hasSize(45);
        assertThat(context.userAgent()).hasSize(255);
        assertThat(new LoginContext("  ", null)).isEqualTo(LoginContext.UNKNOWN);
    }
}

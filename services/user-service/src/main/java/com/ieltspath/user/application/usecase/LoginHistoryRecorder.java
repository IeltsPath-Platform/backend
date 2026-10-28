package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.domain.aggregate.LoginAttempt;
import com.ieltspath.user.domain.repository.LoginHistoryRepository;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Writes login history in its own transaction; a failed write is logged and never fails the sign-in. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryRecorder {
    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional
    public void recordSuccess(UUID userId, LoginContext context) {
        record(LoginAttempt.succeeded(userId, context.ipAddress(), context.userAgent()));
    }

    @Transactional
    public void recordFailure(UUID userId, LoginFailureReason reason, LoginContext context) {
        record(LoginAttempt.failed(userId, reason, context.ipAddress(), context.userAgent()));
    }

    private void record(LoginAttempt attempt) {
        try {
            loginHistoryRepository.save(attempt);
        } catch (RuntimeException exception) {
            log.warn("Không ghi được lịch sử đăng nhập userId={} success={}", attempt.getUserId(), attempt.isSuccess(), exception);
        }
    }
}

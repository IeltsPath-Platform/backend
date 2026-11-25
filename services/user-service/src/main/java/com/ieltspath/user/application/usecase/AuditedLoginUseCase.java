package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.port.LoginAttemptLimiter;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import com.ieltspath.user.application.port.AuthMetrics;
import com.ieltspath.user.application.exception.LoginRateLimitedException;

/**
 * Signs in through {@link LoginUseCase} and records the attempt for the account it targeted. The record is written
 * after the sign-in settles, so a rejected sign-in (which rolls back its own transaction) still leaves its trace.
 * Attempts for emails that match no account are not recorded.
 */
@Service
@RequiredArgsConstructor
public class AuditedLoginUseCase {
    private final LoginUseCase loginUseCase;
    private final UserRepository userRepository;
    private final LoginHistoryRecorder recorder;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private AuthMetrics authMetrics;

    @Autowired
    void setAuthMetrics(AuthMetrics authMetrics) { this.authMetrics = authMetrics; }

    public AuthTokenResult execute(String email, String password, LoginContext context) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String clientIp = context == null ? null : context.ipAddress();
        try {
            loginAttemptLimiter.checkAllowed(normalizedEmail, clientIp);
        } catch (LoginRateLimitedException limited) {
            if (authMetrics != null) authMetrics.record("auth.login", "result", "rate_limited");
            throw limited;
        }
        AuthTokenResult tokens;
        try {
            tokens = loginUseCase.execute(email, password);
        } catch (AuthenticationFailedException exception) {
            loginAttemptLimiter.recordFailure(normalizedEmail, clientIp);
            String[] result = {"bad_credentials"};
            findAccount(email).ifPresent(user -> {
                recorder.recordFailure(user.getId(), failureReason(user), context);
                if (user.getStatus() == UserStatus.LOCKED) result[0] = "locked";
                else if (user.getStatus() != UserStatus.ACTIVE) result[0] = "inactive";
            });
            if (authMetrics != null) authMetrics.record("auth.login", "result", result[0]);
            throw exception;
        }
        loginAttemptLimiter.recordSuccess(normalizedEmail, clientIp);
        findAccount(email).ifPresent(user -> recorder.recordSuccess(user.getId(), context));
        if (authMetrics != null) authMetrics.record("auth.login", "result", "success");
        return tokens;
    }

    private LoginFailureReason failureReason(User user) {
        return user.getStatus() == UserStatus.ACTIVE
                ? LoginFailureReason.BAD_PASSWORD
                : LoginFailureReason.forInactiveStatus(user.getStatus());
    }

    private Optional<User> findAccount(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        try {
            return userRepository.findByEmail(new Email(email).value());
        } catch (IllegalArgumentException invalidEmail) {
            return Optional.empty();
        }
    }
}

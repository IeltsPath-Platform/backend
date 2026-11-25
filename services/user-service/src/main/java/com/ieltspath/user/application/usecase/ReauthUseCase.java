package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.exception.GoogleNonceRejectedException;
import com.ieltspath.user.application.exception.GoogleSignInUnavailableException;
import com.ieltspath.user.application.exception.ReauthAccountMismatchException;
import com.ieltspath.user.application.exception.ReauthRequiredException;
import com.ieltspath.user.application.exception.ReauthUnavailableException;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.port.GoogleNonceStore;
import com.ieltspath.user.application.port.ReauthTokenStore;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.result.ReauthResult;
import com.ieltspath.user.config.GoogleAuthProperties;
import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.exception.PasswordNotConfiguredException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReauthUseCase {
    private final UserRepository users;
    private final OAuthIdentityRepository identities;
    private final PasswordEncoder encoder;
    private final UserLock userLock;
    private final PasswordChangeAttemptRepository attemptsRepository;
    private final GoogleIdTokenVerifier googleVerifier;
    private final GoogleNonceStore nonceStore;
    private final GoogleAuthProperties googleProperties;
    private final ReauthTokenStore reauthTokens;

    @Transactional(noRollbackFor = PasswordChangeException.class)
    public ReauthResult execute(UUID userId, UUID sessionId, String currentPassword, String googleIdToken) {
        if (sessionId == null || (blank(currentPassword) == blank(googleIdToken))) throw new ReauthRequiredException();
        userLock.lock(userId);
        LocalDateTime now = LocalDateTime.now();
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        if (!blank(currentPassword)) {
            verifyPassword(user, currentPassword, now);
            attemptsRepository.deleteByUserId(userId);
            return new ReauthResult(issue(userId, "PASSWORD", sessionId), "PASSWORD");
        }
        var google = verifyGoogle(googleIdToken);
        var linked = identities.findByUserIdAndProvider(userId, OAuthProvider.GOOGLE).orElse(null);
        if (linked == null || !linked.getProviderSubject().equals(google.subject())) {
            throw new ReauthAccountMismatchException();
        }
        return new ReauthResult(issue(userId, "GOOGLE", sessionId), "GOOGLE");
    }

    private void verifyPassword(User user, String currentPassword, LocalDateTime now) {
        PasswordChangeAttempts attempts = attemptsRepository.findByUserId(user.getId())
                .orElseGet(() -> PasswordChangeAttempts.fresh(user.getId(), now));
        if (attempts.isLocked(now)) {
            throw new PasswordChangeException("PASSWORD_ATTEMPTS_LIMITED",
                    "Đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút.", true);
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) throw new PasswordNotConfiguredException();
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            attempts.recordFailure(now);
            attemptsRepository.save(attempts);
            throw new PasswordChangeException("CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không chính xác");
        }
    }

    private com.ieltspath.user.application.result.GoogleIdentity verifyGoogle(String idToken) {
        com.ieltspath.user.application.result.GoogleIdentity identity;
        try {
            identity = googleVerifier.verify(idToken);
        } catch (AuthenticationFailedException invalid) {
            throw new ReauthRequiredException();
        }
        String nonce = identity.nonce();
        if (blank(nonce)) {
            if (googleProperties.isRequireNonce()) throw new ReauthRequiredException();
            return identity;
        }
        try {
            if (!nonceStore.consume(nonce)) throw new ReauthRequiredException();
        } catch (ReauthRequiredException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            if (googleProperties.isRequireNonce()) throw new GoogleSignInUnavailableException(unavailable);
        }
        return identity;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private String issue(UUID userId, String method, UUID sessionId) {
        try { return reauthTokens.issue(userId, method, sessionId); }
        catch (RuntimeException unavailable) { throw new ReauthUnavailableException(); }
    }
}

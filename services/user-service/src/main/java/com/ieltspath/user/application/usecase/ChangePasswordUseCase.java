package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ChangePasswordCommand;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.exception.PasswordNotConfiguredException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.PasswordChangeAttemptRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final UserLock userLock;
    private final PasswordChangeAttemptRepository attemptsRepository;

    /**
     * A wrong password ends in an exception, and the failure it just counted must still be saved, so that exception
     * does not roll the transaction back. The per-user lock makes counting safe across service instances.
     */
    @Transactional(noRollbackFor = PasswordChangeException.class)
    public void execute(ChangePasswordCommand command) {
        UUID userId = command.userId();
        userLock.lock(userId);
        LocalDateTime now = LocalDateTime.now();
        PasswordChangeAttempts attempts = attemptsRepository.findByUserId(userId)
                .orElseGet(() -> PasswordChangeAttempts.fresh(userId, now));
        if (attempts.isLocked(now)) {
            throw new PasswordChangeException("PASSWORD_ATTEMPTS_LIMITED",
                    "Đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút.", true);
        }
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) throw new PasswordNotConfiguredException();
        if (!encoder.matches(command.currentPassword(), user.getPasswordHash())) {
            attempts.recordFailure(now);
            attemptsRepository.save(attempts);
            throw new PasswordChangeException("CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không chính xác");
        }
        if (encoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new PasswordChangeException("PASSWORD_UNCHANGED", "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.changePassword(encoder.encode(command.newPassword()));
        users.save(user);
        refreshTokens.revokeAllActiveForUser(userId, LocalDateTime.now());
        attemptsRepository.deleteByUserId(userId);
    }
}

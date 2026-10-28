package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ChangePasswordCommand;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.PasswordNotConfiguredException;
import com.ieltspath.user.domain.exception.PasswordChangeException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 15 * 60 * 1000L;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final StudySessionRepository studySessions;
    private final ConcurrentHashMap<UUID, Attempts> attempts = new ConcurrentHashMap<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    @Transactional
    public void execute(ChangePasswordCommand command) {
        UUID userId = command.userId();
        long now = System.currentTimeMillis();
        if (requestCount.incrementAndGet() % 256 == 0) {
            attempts.entrySet().removeIf(entry -> now - entry.getValue().windowStartedAt >= LOCK_MILLIS
                    && entry.getValue().lockedUntil <= now);
        }
        Attempts state = attempts.compute(userId, (id, old) -> activeWindow(old, now));
        if (state.failures >= MAX_FAILURES) throw new PasswordChangeException("PASSWORD_ATTEMPTS_LIMITED", "Đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút.", true);
        studySessions.lockUser(userId);
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) throw new PasswordNotConfiguredException();
        if (!encoder.matches(command.currentPassword(), user.getPasswordHash())) {
            attempts.compute(userId, (id, old) -> {
                Attempts next = activeWindow(old, now);
                if (next.lockedUntil > now) return next;
                int failures = next.failures + 1;
                return new Attempts(failures, next.windowStartedAt, failures >= MAX_FAILURES ? now + LOCK_MILLIS : 0);
            });
            throw new PasswordChangeException("CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không chính xác");
        }
        if (encoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new PasswordChangeException("PASSWORD_UNCHANGED", "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.changePassword(encoder.encode(command.newPassword()));
        users.save(user);
        refreshTokens.revokeAllActiveForUser(userId, LocalDateTime.now());
        attempts.remove(userId);
    }

    private Attempts activeWindow(Attempts old, long now) {
        if (old == null) return new Attempts(0, now, 0);
        if (old.lockedUntil > now) return old;
        if ((old.lockedUntil > 0 && old.lockedUntil <= now) || now - old.windowStartedAt >= LOCK_MILLIS) {
            return new Attempts(0, now, 0);
        }
        return old;
    }

    private record Attempts(int failures, long windowStartedAt, long lockedUntil) { }
}

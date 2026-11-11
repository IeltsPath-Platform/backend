package com.ieltspath.user.application.usecase;

import com.ieltspath.events.user.PasswordChangedV1;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import com.ieltspath.user.application.command.ResetPasswordCommand;
import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidActionTokenException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {
    private final UserRepository userRepository;
    private final AccountActionTokenRepository accountActionTokenRepository;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final UserLock userLock;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserNoticeOutbox notices;

    @Transactional
    public void execute(ResetPasswordCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            throw new IllegalArgumentException("Token không được để trống");
        }
        if (command.newPassword() == null || command.newPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống");
        }

        String tokenHash = tokenHashService.hash(command.token().trim());
        AccountActionToken actionToken = accountActionTokenRepository.findByTokenHashAndPurpose(tokenHash, ActionTokenPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidActionTokenException("Mã token đặt lại mật khẩu không hợp lệ hoặc không tồn tại"));

        if (!actionToken.isValid()) {
            throw new InvalidActionTokenException("Mã token đã hết hạn hoặc đã được sử dụng");
        }

        User user = userRepository.findById(actionToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng sở hữu token này"));
        userLock.lock(user.getId());
        user = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng sở hữu token này"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidActionTokenException("Không thể đặt lại mật khẩu cho tài khoản không hoạt động");
        }
        if (!accountActionTokenRepository.consumePasswordResetToken(tokenHash, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))) {
            throw new InvalidActionTokenException("Mã token đã hết hạn hoặc đã được sử dụng");
        }

        boolean wasVerified = user.isEmailVerified();
        user.markEmailVerified(java.time.LocalDateTime.now());
        if (!wasVerified) {
            UUID stateId = UUID.randomUUID();
            notices.enqueue(stateId, user.getId(), UserEmailVerificationStateV1.EVENT_TYPE, new UserEmailVerificationStateV1(
                    stateId, UserEmailVerificationStateV1.EVENT_TYPE, Instant.now(), user.getId(), true,
                    user.getEmail().value(), user.getFullName()));
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(user.getId(), java.time.LocalDateTime.now());

        UUID eventId = UUID.randomUUID();
        notices.enqueue(eventId, user.getId(), PasswordChangedV1.EVENT_TYPE, new PasswordChangedV1(eventId,
                PasswordChangedV1.EVENT_TYPE, Instant.now(), user.getId(), user.getEmail().value(), user.getFullName()));
        log.info("Đặt lại mật khẩu thành công cho userId={}", user.getId());
    }
}


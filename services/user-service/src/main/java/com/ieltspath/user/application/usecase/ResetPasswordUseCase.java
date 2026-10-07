package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.ResetPasswordCommand;
import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidActionTokenException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {
    private final UserRepository userRepository;
    private final AccountActionTokenRepository accountActionTokenRepository;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;

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

        actionToken.use();
        accountActionTokenRepository.save(actionToken);

        User user = userRepository.findById(actionToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng sở hữu token này"));

        user.changePassword(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);

        log.info("Đặt lại mật khẩu thành công cho userId={}", user.getId());
    }
}


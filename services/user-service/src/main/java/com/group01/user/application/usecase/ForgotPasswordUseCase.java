package com.group01.user.application.usecase;

import com.group01.user.application.command.ForgotPasswordCommand;
import com.group01.user.domain.aggregate.AccountActionToken;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.AccountActionTokenRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.ActionTokenPurpose;
import com.group01.user.domain.vo.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {
    private final UserRepository userRepository;
    private final AccountActionTokenRepository accountActionTokenRepository;
    private final TokenHashService tokenHashService;

    @Transactional
    public String execute(ForgotPasswordCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }

        User user = userRepository.findByEmail(command.email().trim())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với email: " + command.email()));

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = tokenHashService.hash(rawToken);

        AccountActionToken actionToken = AccountActionToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .purpose(ActionTokenPurpose.PASSWORD_RESET)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();

        accountActionTokenRepository.save(actionToken);
        log.info("Đã tạo token đặt lại mật khẩu cho userId={}", user.getId());

        return rawToken;
    }
}

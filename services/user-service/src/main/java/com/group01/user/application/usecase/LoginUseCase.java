package com.group01.user.application.usecase;

import com.group01.user.application.result.AuthTokenResult;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.AuthenticationFailedException;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;

    @Transactional
    public AuthTokenResult execute(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new AuthenticationFailedException("Email hoặc mật khẩu không đúng");
        }
        if (password == null || password.isBlank()) {
            throw new AuthenticationFailedException("Email hoặc mật khẩu không đúng");
        }
        Email normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail.value())
                .orElseThrow(() -> new AuthenticationFailedException("Email hoặc mật khẩu không đúng"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Email hoặc mật khẩu không đúng");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Email hoặc mật khẩu không đúng");
        }
        return authTokenIssuer.issue(user);
    }

    private Email normalizeEmail(String email) {
        try {
            return new Email(email);
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException("Email hoặc mật khẩu không đúng");
        }
    }
}

package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.port.PasswordRehashMetrics;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordEncoder legacyPasswordEncoder;
    private final AuthTokenIssuer authTokenIssuer;
    private final PasswordRehashMetrics rehashMetrics;
    private final String dummyHash;
    private final String legacyDummyHash;

    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        @Qualifier("legacyPasswordEncoder") PasswordEncoder legacyPasswordEncoder,
                        AuthTokenIssuer authTokenIssuer, PasswordRehashMetrics rehashMetrics) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.legacyPasswordEncoder = legacyPasswordEncoder;
        this.authTokenIssuer = authTokenIssuer;
        this.rehashMetrics = rehashMetrics;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        this.legacyDummyHash = legacyPasswordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthTokenResult execute(String email, String password) {
        String candidatePassword = password == null ? "" : password;
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return reject(candidatePassword);
        }
        Email normalizedEmail;
        try {
            normalizedEmail = new Email(email);
        } catch (IllegalArgumentException exception) {
            return reject(candidatePassword);
        }
        User user = userRepository.findByEmail(normalizedEmail.value())
                .orElse(null);
        if (user == null) return reject(candidatePassword);
        if (user.getStatus() != UserStatus.ACTIVE) {
            return reject(candidatePassword);
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return reject(candidatePassword);
        }
        if (!passwordEncoder.matches(candidatePassword, user.getPasswordHash())) {
            if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
                passwordEncoder.matches(candidatePassword, dummyHash);
            } else {
                legacyPasswordEncoder.matches(candidatePassword, legacyDummyHash);
            }
            throw new AuthenticationFailedException("Email hoặc mật khẩu không đúng");
        }
        String oldHash = user.getPasswordHash();
        if (passwordEncoder.upgradeEncoding(oldHash)) {
            String upgradedHash = passwordEncoder.encode(candidatePassword);
            if (userRepository.updatePasswordHashIfUnchanged(user.getId(), oldHash, upgradedHash) == 1) {
                rehashMetrics.recordUpgrade();
            }
        }
        return authTokenIssuer.issue(user);
    }

    private AuthTokenResult reject(String password) {
        passwordEncoder.matches(password, dummyHash);
        legacyPasswordEncoder.matches(password, legacyDummyHash);
        throw new AuthenticationFailedException("Email hoặc mật khẩu không đúng");
    }
}

package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.domain.aggregate.RefreshToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenHashService tokenHashService;
    private final AuthTokenIssuer authTokenIssuer;

    @Transactional
    public AuthTokenResult execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }
        LocalDateTime now = LocalDateTime.now();
        RefreshToken storedToken = refreshTokenRepository.consumeActiveToken(tokenHashService.hash(refreshToken), now)
                .orElseThrow(() -> new AuthenticationFailedException("Refresh token không hợp lệ"));
        if (!storedToken.isUsable(now)) {
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + storedToken.getUserId()));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Refresh token không hợp lệ");
        }

        return authTokenIssuer.issue(user);
    }
}

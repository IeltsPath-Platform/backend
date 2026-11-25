package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.OAuthIdentityNotFoundException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.events.user.GoogleAccountUnlinkedV1;
import com.ieltspath.user.application.exception.ReauthRequiredException;
import com.ieltspath.user.application.port.ReauthTokenStore;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UnlinkOAuthIdentityUseCase {
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final UserRepository userRepository;
    private final ReauthTokenStore reauthTokens;
    private final UserNoticeOutbox notices;
    @Value("${app.auth.reauth-required-for-unlink:false}") private boolean reauthRequired;

    public void execute(UUID userId, String providerStr) { execute(userId, providerStr, null, null); }

    @Transactional
    public void execute(UUID userId, String providerStr, String reauthToken, UUID sessionId) {
        if (providerStr == null || providerStr.isBlank()) {
            throw new IllegalArgumentException("Nhà cung cấp OAuth không được để trống");
        }

        if (reauthRequired) requireReauth(userId, reauthToken, sessionId);

        OAuthProvider provider;
        try {
            provider = OAuthProvider.valueOf(providerStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nhà cung cấp OAuth không hợp lệ");
        }

        oauthIdentityRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new OAuthIdentityNotFoundException("Không tìm thấy liên kết OAuth cho nhà cung cấp: " + provider));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với id: " + userId));

        // Bảo vệ an toàn tài khoản: Nếu user chưa tạo mật khẩu và chỉ còn duy nhất 1 liên kết OAuth thì không cho gỡ
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            List<OAuthIdentity> allIdentities = oauthIdentityRepository.findByUserId(userId);
            if (allIdentities.size() <= 1) {
                throw new IllegalStateException("Không thể hủy liên kết tài khoản OAuth duy nhất khi chưa thiết lập mật khẩu");
            }
        }

        oauthIdentityRepository.deleteByUserIdAndProvider(userId, provider);
        if (provider == OAuthProvider.GOOGLE) {
            UUID eventId = UUID.randomUUID();
            notices.enqueue(eventId, userId, GoogleAccountUnlinkedV1.EVENT_TYPE, new GoogleAccountUnlinkedV1(
                    eventId, GoogleAccountUnlinkedV1.EVENT_TYPE, Instant.now(), userId,
                    user.getEmail().value(), user.getFullName()));
        }
    }

    private void requireReauth(UUID userId, String token, UUID sessionId) {
        if (sessionId == null || token == null || token.isBlank()) throw new ReauthRequiredException();
        try {
            var grant = reauthTokens.consume(token).orElse(null);
            if (grant == null || !grant.userId().equals(userId) || !grant.sessionId().equals(sessionId)) {
                throw new ReauthRequiredException();
            }
        } catch (ReauthRequiredException rejected) {
            throw rejected;
        } catch (RuntimeException unavailable) {
            throw new ReauthRequiredException();
        }
    }
}


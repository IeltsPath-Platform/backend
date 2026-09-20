package com.group01.user.application.usecase;

import com.group01.user.domain.aggregate.OAuthIdentity;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.OAuthIdentityNotFoundException;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.OAuthIdentityRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnlinkOAuthIdentityUseCase {
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final UserRepository userRepository;

    @Transactional
    public void execute(UUID userId, String providerStr) {
        if (providerStr == null || providerStr.isBlank()) {
            throw new IllegalArgumentException("Nhà cung cấp OAuth không được để trống");
        }

        OAuthProvider provider;
        try {
            provider = OAuthProvider.valueOf(providerStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nhà cung cấp OAuth không hợp lệ: " + providerStr);
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
    }
}


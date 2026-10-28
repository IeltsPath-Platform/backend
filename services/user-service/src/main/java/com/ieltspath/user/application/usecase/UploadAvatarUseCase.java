package com.ieltspath.user.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.application.port.AvatarUploadRateLimiter;
import com.ieltspath.user.application.result.LearnerProfileResult;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadAvatarUseCase {
    private static final UploadPolicy POLICY = UploadPolicy.of(2L * 1024 * 1024,
            "image/jpeg", "image/png", "image/webp");
    private final AvatarStoragePort storage;
    private final AvatarUploadRateLimiter rateLimiter;
    private final LearnerProfileRepository profiles;
    private final UserRepository users;
    @Value("${app.user.avatar.bucket:avatars}") private String bucket;
    @Value("${app.storage.supabase.url:}") private String projectUrl;

    public LearnerProfileResult execute(UUID userId, byte[] bytes, String declaredType) {
        rateLimiter.checkAndAcquire(userId);
        String mime = POLICY.check(bytes, declaredType);
        String key = ObjectKeys.forUser("images", userId, ContentTypes.extensionFor(mime));
        String publicUrl = storage.put(key, bytes, mime);
        try {
            LearnerProfile profile = profiles.findByUserId(userId).orElseGet(() -> defaultProfile(userId));
            String oldReference = profile.getAvatarReference();
            profile.updateAvatarReference(publicUrl);
            LearnerProfile saved = profiles.save(profile);
            AvatarReferences.ownedKey(oldReference, projectUrl, bucket, userId).ifPresent(oldKey -> {
                if (!oldKey.equals(key)) deleteBestEffort(oldKey);
            });
            return result(saved);
        } catch (RuntimeException e) {
            deleteBestEffort(key);
            throw e;
        }
    }

    private LearnerProfile defaultProfile(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return LearnerProfile.builder().userId(userId)
                .displayName(user.getFullName() == null || user.getFullName().isBlank() ? "Learner" : user.getFullName())
                .timezone("Asia/Ho_Chi_Minh").visibility(ProfileVisibility.PUBLIC)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private void deleteBestEffort(String key) { try { storage.delete(key); } catch (RuntimeException ignored) { } }
    static LearnerProfileResult result(LearnerProfile p) {
        return new LearnerProfileResult(p.getUserId(), p.getDisplayName(), p.getAvatarReference(), p.getBio(),
                p.getSelfReportedBand(), p.getTimezone(), p.getVisibility().name(), p.getCreatedAt(), p.getUpdatedAt());
    }
}

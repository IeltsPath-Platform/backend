package com.ieltspath.user.application.usecase;

import com.ieltspath.commonstorage.key.ObjectKeys;
import com.ieltspath.commonstorage.upload.ContentTypes;
import com.ieltspath.commonstorage.upload.UploadPolicy;
import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.application.port.AvatarUploadRateLimiter;
import com.ieltspath.user.application.result.LearnerProfileResult;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadAvatarUseCase {
    private static final UploadPolicy POLICY = UploadPolicy.of(2L * 1024 * 1024,
            "image/jpeg", "image/png", "image/webp");
    private final AvatarStoragePort storage;
    private final AvatarUploadRateLimiter rateLimiter;
    private final AvatarReferenceWriter writer;
    @Value("${app.user.avatar.bucket:avatars}") private String bucket;
    @Value("${app.storage.supabase.url:}") private String projectUrl;

    public LearnerProfileResult execute(UUID userId, byte[] bytes, String declaredType) {
        rateLimiter.checkAndAcquire(userId);
        String mime = POLICY.check(bytes, declaredType);
        String key = ObjectKeys.forUser("images", userId, ContentTypes.extensionFor(mime));
        String publicUrl = storage.put(key, bytes, mime);
        AvatarReferenceWriter.Outcome outcome;
        try {
            outcome = writer.replace(userId, publicUrl, key);
        } catch (RuntimeException e) {
            deleteBestEffort(key);
            throw e;
        }
        // The account was deleted while the file was uploading: the writer queued the new object for removal.
        if (outcome.accountDeleted()) throw accountDeleted();
        AvatarReferences.ownedKey(outcome.previousReference(), projectUrl, bucket, userId).ifPresent(oldKey -> {
            if (!oldKey.equals(key)) deleteBestEffort(oldKey);
        });
        return result(outcome.profile());
    }

    private void deleteBestEffort(String key) { try { storage.delete(key); } catch (RuntimeException ignored) { } }

    static UserManagementRuleViolationException accountDeleted() {
        return new UserManagementRuleViolationException("ACCOUNT_DELETED", "Tài khoản này đã bị xóa nên không thể thay đổi.");
    }

    static LearnerProfileResult result(LearnerProfile p) {
        return new LearnerProfileResult(p.getUserId(), p.getDisplayName(), p.getAvatarReference(), p.getBio(),
                p.getSelfReportedBand(), p.getTimezone(), p.getVisibility().name(), p.getCreatedAt(), p.getUpdatedAt());
    }
}

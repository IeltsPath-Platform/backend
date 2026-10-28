package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.application.result.LearnerProfileResult;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteAvatarUseCase {
    private final AvatarStoragePort storage;
    private final LearnerProfileRepository profiles;
    private final UserRepository users;
    @Value("${app.user.avatar.bucket:avatars}") private String bucket;
    @Value("${app.storage.supabase.url:}") private String projectUrl;

    public LearnerProfileResult execute(UUID userId) {
        LearnerProfile profile = profiles.findByUserId(userId).orElseGet(() -> defaultProfile(userId));
        String oldReference = profile.getAvatarReference();
        profile.updateAvatarReference(null);
        LearnerProfile saved = profiles.save(profile);
        AvatarReferences.ownedKey(oldReference, projectUrl, bucket, userId).ifPresent(key -> {
            try { storage.delete(key); } catch (RuntimeException ignored) { }
        });
        return UploadAvatarUseCase.result(saved);
    }

    private LearnerProfile defaultProfile(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return LearnerProfile.createDefault(userId, user.getFullName());
    }
}

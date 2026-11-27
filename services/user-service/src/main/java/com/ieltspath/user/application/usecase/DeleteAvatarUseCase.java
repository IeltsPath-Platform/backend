package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.AvatarStoragePort;
import com.ieltspath.user.application.result.LearnerProfileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteAvatarUseCase {
    private final AvatarStoragePort storage;
    private final AvatarReferenceWriter writer;
    @Value("${app.user.avatar.bucket:avatars}") private String bucket;
    @Value("${app.storage.supabase.url:}") private String projectUrl;

    public LearnerProfileResult execute(UUID userId) {
        AvatarReferenceWriter.Outcome outcome = writer.replace(userId, null, null);
        if (outcome.accountDeleted()) throw UploadAvatarUseCase.accountDeleted();
        AvatarReferences.ownedKey(outcome.previousReference(), projectUrl, bucket, userId).ifPresent(key -> {
            try { storage.delete(key); } catch (RuntimeException ignored) { }
        });
        return UploadAvatarUseCase.result(outcome.profile());
    }
}

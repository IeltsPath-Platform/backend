package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.AccountDeletionCleanupPort;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes the avatar reference under the account lock that account deletion also takes, so the profile is read after
 * any deletion has committed and never overwrites the anonymised one. Storage calls stay with the callers, outside.
 */
@Service
@RequiredArgsConstructor
public class AvatarReferenceWriter {
    private final UserLock userLock;
    private final UserRepository users;
    private final LearnerProfileRepository profiles;
    private final AccountDeletionCleanupPort cleanupPort;

    /** The saved profile and the reference it replaced; {@code accountDeleted} when nothing was written. */
    public record Outcome(LearnerProfile profile, String previousReference, boolean accountDeleted) { }

    /**
     * Sets the avatar to {@code reference} (null removes it). When the account was deleted meanwhile, a just-uploaded
     * object ({@code uploadedKey}) is queued for removal in this transaction instead, so it does not stay public.
     */
    @Transactional
    public Outcome replace(UUID userId, String reference, String uploadedKey) {
        userLock.lock(userId);
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (user.getStatus() == UserStatus.DELETED) {
            if (uploadedKey != null) cleanupPort.queueAvatarDeletion(userId, uploadedKey);
            return new Outcome(null, null, true);
        }
        LearnerProfile profile = profiles.findByUserId(userId)
                .orElseGet(() -> LearnerProfile.createDefault(userId, user.getFullName()));
        String previous = profile.getAvatarReference();
        profile.updateAvatarReference(reference);
        return new Outcome(profiles.save(profile), previous, false);
    }
}

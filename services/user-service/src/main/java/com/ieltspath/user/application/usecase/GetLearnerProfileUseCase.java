package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.result.LearnerProfileResult;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetLearnerProfileUseCase {
    private final LearnerProfileRepository learnerProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public LearnerProfileResult execute(UUID userId) {
        LearnerProfile profile = learnerProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId));
        return toResult(profile);
    }

    private LearnerProfile createDefaultProfile(UUID userId) {
        String fullName = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với id: " + userId))
                .getFullName();
        return learnerProfileRepository.save(LearnerProfile.createDefault(userId, fullName));
    }

    private LearnerProfileResult toResult(LearnerProfile profile) {
        return new LearnerProfileResult(
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getAvatarReference(),
                profile.getBio(),
                profile.getSelfReportedBand(),
                profile.getTimezone(),
                profile.getVisibility().name(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

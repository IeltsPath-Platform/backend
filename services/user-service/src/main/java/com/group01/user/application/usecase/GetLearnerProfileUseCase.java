package com.group01.user.application.usecase;

import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.domain.aggregate.LearnerProfile;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.LearnerProfileRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.ProfileVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với id: " + userId));

        String displayName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : "Learner";

        LearnerProfile defaultProfile = LearnerProfile.builder()
                .userId(userId)
                .displayName(displayName)
                .timezone("Asia/Ho_Chi_Minh")
                .visibility(ProfileVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return learnerProfileRepository.save(defaultProfile);
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


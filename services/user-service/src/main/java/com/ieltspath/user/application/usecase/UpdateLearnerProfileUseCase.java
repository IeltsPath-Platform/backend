package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.UpdateLearnerProfileCommand;
import com.ieltspath.user.application.result.LearnerProfileResult;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.LearningGoalRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UpdateLearnerProfileUseCase {
    private final LearnerProfileRepository learnerProfileRepository;
    private final UserRepository userRepository;
    private final LearningGoalRepository learningGoalRepository;
    private final ScheduleStudyGoalUseCase studyGoals;
    @Value("${app.user.avatar.bucket:avatars}") private String avatarBucket;
    @Value("${app.storage.supabase.url:}") private String supabaseUrl;

    @Transactional
    public LearnerProfileResult execute(UpdateLearnerProfileCommand command) {
        studyGoals.lock(command.userId());
        LearnerProfile profile = learnerProfileRepository.findByUserId(command.userId())
                .orElseGet(() -> {
                    if (userRepository.findById(command.userId()).isEmpty()) {
                        throw new UserNotFoundException("Không tìm thấy người dùng với id: " + command.userId());
                    }
                    return LearnerProfile.builder()
                            .userId(command.userId())
                            .displayName(command.displayName())
                            .timezone("Asia/Ho_Chi_Minh")
                            .visibility(ProfileVisibility.PUBLIC)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                });

        String previousTimezone = profile.getTimezone();
        if (!Objects.equals(profile.getAvatarReference(), command.avatarReference())
                && command.avatarReference() != null
                && AvatarReferences.ownedKey(command.avatarReference(), supabaseUrl, avatarBucket, command.userId()).isEmpty()) {
            throw new IllegalArgumentException("Ảnh đại diện phải thuộc kho ảnh của tài khoản này");
        }
        profile.updateDetails(
                command.displayName(),
                command.avatarReference(),
                command.bio(),
                command.selfReportedBand(),
                command.timezone()
        );

        if (command.visibility() != null && !command.visibility().isBlank()) {
            profile.changeVisibility(ProfileVisibility.valueOf(command.visibility().trim().toUpperCase()));
        }

        LearnerProfile saved = learnerProfileRepository.save(profile);
        if (!Objects.equals(previousTimezone, saved.getTimezone())) {
            learningGoalRepository.findActiveByUserId(command.userId()).ifPresent(studyGoals::execute);
        }
        return toResult(saved);
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

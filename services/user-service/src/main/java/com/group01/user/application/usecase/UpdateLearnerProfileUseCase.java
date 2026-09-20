package com.group01.user.application.usecase;

import com.group01.user.application.command.UpdateLearnerProfileCommand;
import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.domain.aggregate.LearnerProfile;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.LearnerProfileRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.ProfileVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UpdateLearnerProfileUseCase {
    private final LearnerProfileRepository learnerProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public LearnerProfileResult execute(UpdateLearnerProfileCommand command) {
        LearnerProfile profile = learnerProfileRepository.findByUserId(command.userId())
                .orElseGet(() -> {
                    if (userRepository.findById(command.userId()).isEmpty()) {
                        throw new UserNotFoundException("Không tìm thấy người dùng với id: " + command.userId());
                    }
                    return LearnerProfile.builder()
                            .userId(command.userId())
                            .displayName(command.displayName())
                            .timezone(command.timezone() != null ? command.timezone() : "Asia/Ho_Chi_Minh")
                            .visibility(ProfileVisibility.PUBLIC)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                });

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

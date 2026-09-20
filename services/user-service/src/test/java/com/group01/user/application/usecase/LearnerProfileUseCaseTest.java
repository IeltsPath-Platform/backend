package com.group01.user.application.usecase;

import com.group01.user.application.command.UpdateLearnerProfileCommand;
import com.group01.user.application.result.LearnerProfileResult;
import com.group01.user.domain.aggregate.LearnerProfile;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.exception.UserNotFoundException;
import com.group01.user.domain.repository.LearnerProfileRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.ProfileVisibility;
import com.group01.user.domain.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnerProfileUseCaseTest {
    @Mock private LearnerProfileRepository learnerProfileRepository;
    @Mock private UserRepository userRepository;

    private GetLearnerProfileUseCase getLearnerProfileUseCase;
    private UpdateLearnerProfileUseCase updateLearnerProfileUseCase;
    private DeleteLearnerProfileUseCase deleteLearnerProfileUseCase;

    @BeforeEach
    void setUp() {
        getLearnerProfileUseCase = new GetLearnerProfileUseCase(learnerProfileRepository, userRepository);
        updateLearnerProfileUseCase = new UpdateLearnerProfileUseCase(learnerProfileRepository, userRepository);
        deleteLearnerProfileUseCase = new DeleteLearnerProfileUseCase(learnerProfileRepository);
    }

    @Test
    void getLearnerProfileReturnsExistingProfile() {
        UUID userId = UUID.randomUUID();
        LearnerProfile existing = LearnerProfile.builder()
                .userId(userId)
                .displayName("Nguyen Van A")
                .visibility(ProfileVisibility.PUBLIC)
                .timezone("Asia/Ho_Chi_Minh")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(learnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        LearnerProfileResult result = getLearnerProfileUseCase.execute(userId);
        assertNotNull(result);
        assertEquals("Nguyen Van A", result.displayName());
    }

    @Test
    void getLearnerProfileCreatesDefaultProfileWhenMissing() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(new Email("test@example.com"))
                .fullName("Tran Van B")
                .status(UserStatus.ACTIVE)
                .build();

        when(learnerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(learnerProfileRepository.save(any(LearnerProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LearnerProfileResult result = getLearnerProfileUseCase.execute(userId);
        assertNotNull(result);
        assertEquals("Tran Van B", result.displayName());
        assertEquals("PUBLIC", result.visibility());
    }

    @Test
    void getLearnerProfileThrowsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(learnerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> getLearnerProfileUseCase.execute(userId));
    }

    @Test
    void updateLearnerProfileSavesUpdatedDetails() {
        UUID userId = UUID.randomUUID();
        LearnerProfile existing = LearnerProfile.builder()
                .userId(userId)
                .displayName("Old Name")
                .visibility(ProfileVisibility.PUBLIC)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(learnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(learnerProfileRepository.save(any(LearnerProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateLearnerProfileCommand cmd = new UpdateLearnerProfileCommand(
                userId,
                "New Name",
                "avatar.png",
                "My bio",
                BigDecimal.valueOf(7.5),
                "UTC",
                "PRIVATE"
        );

        LearnerProfileResult result = updateLearnerProfileUseCase.execute(cmd);
        assertEquals("New Name", result.displayName());
        assertEquals("avatar.png", result.avatarReference());
        assertEquals("My bio", result.bio());
        assertEquals(BigDecimal.valueOf(7.5), result.selfReportedBand());
        assertEquals("UTC", result.timezone());
        assertEquals("PRIVATE", result.visibility());
    }

    @Test
    void deleteLearnerProfileDeletesByUserId() {
        UUID userId = UUID.randomUUID();
        deleteLearnerProfileUseCase.execute(userId);
        verify(learnerProfileRepository).deleteByUserId(userId);
    }
}


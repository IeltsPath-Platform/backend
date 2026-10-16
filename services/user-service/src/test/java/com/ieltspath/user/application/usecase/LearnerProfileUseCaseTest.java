package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.UpdateLearnerProfileCommand;
import com.ieltspath.user.application.result.LearnerProfileResult;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.aggregate.LearningGoal;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import com.ieltspath.user.domain.repository.LearningGoalRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import com.ieltspath.user.domain.vo.UserStatus;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LearnerProfileUseCaseTest {
    @Mock private LearnerProfileRepository learnerProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private LearningGoalRepository learningGoalRepository;
    @Mock private ScheduleStudyGoalUseCase studyGoals;

    private GetLearnerProfileUseCase getLearnerProfileUseCase;
    private UpdateLearnerProfileUseCase updateLearnerProfileUseCase;
    private DeleteLearnerProfileUseCase deleteLearnerProfileUseCase;

    @BeforeEach
    void setUp() {
        getLearnerProfileUseCase = new GetLearnerProfileUseCase(learnerProfileRepository, userRepository);
        updateLearnerProfileUseCase = new UpdateLearnerProfileUseCase(learnerProfileRepository, userRepository,
                learningGoalRepository, studyGoals);
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
    void timezoneChangeLocksBeforeReadingAndReschedulesActiveGoalAfterSavingProfile() {
        UUID userId = UUID.randomUUID();
        LearnerProfile existing = existingProfile(userId, "Asia/Ho_Chi_Minh");
        LearningGoal goal = LearningGoal.builder().userId(userId).build();
        when(learnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(learnerProfileRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.of(goal));

        var result = updateLearnerProfileUseCase.execute(timezoneCommand(userId, " America/Los_Angeles "));

        assertEquals("America/Los_Angeles", result.timezone());
        var order = inOrder(studyGoals, learnerProfileRepository, learningGoalRepository);
        order.verify(studyGoals).lock(userId);
        order.verify(learnerProfileRepository).findByUserId(userId);
        order.verify(learnerProfileRepository).save(existing);
        order.verify(learningGoalRepository).findActiveByUserId(userId);
        order.verify(studyGoals).execute(goal);
    }

    @Test
    void unchangedOrOmittedTimezoneDoesNotRescheduleGoal() {
        UUID userId = UUID.randomUUID();
        LearnerProfile existing = existingProfile(userId, "UTC");
        when(learnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(learnerProfileRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        for (String timezone : new String[]{" UTC ", null, " "}) {
            assertEquals("UTC", updateLearnerProfileUseCase.execute(timezoneCommand(userId, timezone)).timezone());
        }

        verifyNoInteractions(learningGoalRepository);
        verify(studyGoals, never()).execute(any());
    }

    @Test
    void changedTimezoneWithoutActiveGoalDoesNotScheduleATarget() {
        UUID userId = UUID.randomUUID();
        LearnerProfile existing = existingProfile(userId, "UTC");
        when(learnerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(learnerProfileRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
        when(learningGoalRepository.findActiveByUserId(userId)).thenReturn(Optional.empty());

        updateLearnerProfileUseCase.execute(timezoneCommand(userId, "Asia/Ho_Chi_Minh"));

        verify(studyGoals, never()).execute(any());
    }

    private LearnerProfile existingProfile(UUID userId, String timezone) {
        return LearnerProfile.builder().userId(userId).displayName("Learner")
                .visibility(ProfileVisibility.PUBLIC).timezone(timezone).build();
    }

    private UpdateLearnerProfileCommand timezoneCommand(UUID userId, String timezone) {
        return new UpdateLearnerProfileCommand(userId, "Learner", null, null, null, timezone, null);
    }

    @Test
    void deleteLearnerProfileDeletesByUserId() {
        UUID userId = UUID.randomUUID();
        deleteLearnerProfileUseCase.execute(userId);
        verify(learnerProfileRepository).deleteByUserId(userId);
    }
}


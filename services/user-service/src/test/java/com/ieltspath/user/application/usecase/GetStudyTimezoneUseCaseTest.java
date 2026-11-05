package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.repository.LearnerProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The timezone a learner's study days are counted in: the one on their profile, or the product default. */
class GetStudyTimezoneUseCaseTest {
    private final UUID user = UUID.randomUUID();
    private final LearnerProfileRepository profiles = mock(LearnerProfileRepository.class);
    private final GetStudyTimezoneUseCase useCase = new GetStudyTimezoneUseCase(profiles);

    private void profileWith(String timezone) {
        when(profiles.findByUserId(user)).thenReturn(Optional.of(LearnerProfile.builder().userId(user).timezone(timezone).build()));
    }

    @Test
    void usesTheTimezoneOnTheProfile() {
        profileWith("America/Los_Angeles");

        assertEquals("America/Los_Angeles", useCase.execute(user));
    }

    @Test
    void aLearnerWithoutAProfileGetsTheDefault() {
        when(profiles.findByUserId(user)).thenReturn(Optional.empty());

        assertEquals("Asia/Ho_Chi_Minh", useCase.execute(user));
    }

    @Test
    void aMissingOrInvalidTimezoneGetsTheDefault() {
        profileWith(null);
        assertEquals("Asia/Ho_Chi_Minh", useCase.execute(user));

        profileWith("Mars/Base");
        assertEquals("Asia/Ho_Chi_Minh", useCase.execute(user));
    }
}

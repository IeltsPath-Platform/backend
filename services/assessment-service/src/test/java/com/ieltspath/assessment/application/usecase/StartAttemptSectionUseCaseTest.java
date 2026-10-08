package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartAttemptSectionUseCaseTest {
    @Mock AssessmentAttemptRepository attempts;
    @Mock AttemptSectionRepository sections;

    private final UUID userId = UUID.randomUUID();
    private final AssessmentAttempt attempt = attempt();

    @Test
    void theFirstOpeningIsRecorded() {
        AttemptSection section = new AttemptSection(UUID.randomUUID(), attempt.getId(), UUID.randomUUID(), 1, "{}");
        arrange(section);

        useCase().execute(userId, attempt.getId(), section.id());

        ArgumentCaptor<AttemptSection> saved = ArgumentCaptor.forClass(AttemptSection.class);
        verify(sections).save(saved.capture());
        assertThat(saved.getValue().startedAt()).isNotNull();
        assertThat(saved.getValue().completedAt()).isNull();
    }

    @Test
    void reopeningKeepsTheFirstStart() {
        AttemptSection started = new AttemptSection(UUID.randomUUID(), attempt.getId(), UUID.randomUUID(), 1, "{}",
                Instant.now().minusSeconds(600), null);
        arrange(started);

        useCase().execute(userId, attempt.getId(), started.id());

        verify(sections, never()).save(any());
    }

    @Test
    void aFinishedSectionIsLeftAlone() {
        AttemptSection done = new AttemptSection(UUID.randomUUID(), attempt.getId(), UUID.randomUUID(), 1, "{}",
                null, Instant.now());
        arrange(done);

        useCase().execute(userId, attempt.getId(), done.id());

        verify(sections, never()).save(any());
    }

    @Test
    void aSectionOfAnotherAttemptIsNotFound() {
        AttemptSection foreign = new AttemptSection(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "{}");
        arrange(foreign);

        assertThatThrownBy(() -> useCase().execute(userId, attempt.getId(), foreign.id()))
                .isInstanceOf(AssessmentNotFoundException.class);
    }

    private void arrange(AttemptSection section) {
        when(attempts.findByIdAndUserId(attempt.getId(), userId)).thenReturn(Optional.of(attempt));
        when(sections.findById(section.id())).thenReturn(Optional.of(section));
    }

    private StartAttemptSectionUseCase useCase() {
        return new StartAttemptSectionUseCase(attempts, sections);
    }

    private AssessmentAttempt attempt() {
        Instant now = Instant.now();
        return new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(), AttemptType.PLACEMENT,
                AttemptMode.STANDARD, AttemptChannel.WEB, AttemptStatus.IN_PROGRESS, now, null, null, 1, now, now);
    }
}

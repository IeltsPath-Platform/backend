package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.CourseMilestoneCountRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A request reads the milestones from Content and keeps the count; an event, which has no learner token, uses it. */
class CourseMilestonesTest {
    private final UUID course = UUID.randomUUID();
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final CourseMilestoneCountRepository counts = mock(CourseMilestoneCountRepository.class);
    private final CourseMilestones milestones = new CourseMilestones(content, counts);

    private static LearningContentClient.PracticeTestMilestone milestone() {
        return mock(LearningContentClient.PracticeTestMilestone.class);
    }

    @Test
    void aRequestReadsTheCountFromContent() {
        when(content.getPracticeTestMilestones(course)).thenReturn(List.of(milestone(), milestone()));

        assertEquals(2, milestones.count(course));

        verify(counts, never()).find(any());
    }

    @Test
    void keepingStoresTheCurrentCount() {
        when(content.getPracticeTestMilestones(course)).thenReturn(List.of(milestone(), milestone()));

        milestones.keep(course);

        verify(counts).save(course, 2);
    }

    @Test
    void anEventWithoutALearnerTokenUsesTheKeptCount() {
        when(content.getPracticeTestMilestones(course))
                .thenThrow(new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required"));
        when(counts.find(course)).thenReturn(Optional.of(3));

        assertEquals(3, milestones.count(course));

        verify(counts, never()).save(any(), anyInt());
    }

    @Test
    void withoutATokenAndNothingKeptTheFailureStandsSoTheEventIsRetried() {
        var unauthorized = new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required");
        when(content.getPracticeTestMilestones(course)).thenThrow(unauthorized);
        when(counts.find(course)).thenReturn(Optional.empty());

        assertSame(unauthorized, assertThrows(LearningRequestException.class, () -> milestones.count(course)));
    }

    @Test
    void aContentOutageIsNotHiddenBehindAKeptCount() {
        when(content.getPracticeTestMilestones(course))
                .thenThrow(new LearningRequestException(503, "CONTENT_UNAVAILABLE", "Learning content is unavailable"));

        assertEquals(503, assertThrows(LearningRequestException.class, () -> milestones.count(course)).getStatus());
        verify(counts, never()).find(any());
    }
}

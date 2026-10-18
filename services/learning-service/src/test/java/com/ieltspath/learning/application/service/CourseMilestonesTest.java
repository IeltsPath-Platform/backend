package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.CourseMilestoneCountRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** A request reads the milestones from Content and keeps the count; an event reads only what was kept. */
class CourseMilestonesTest {
    private final UUID course = UUID.randomUUID();
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final CourseMilestoneCountRepository counts = mock(CourseMilestoneCountRepository.class);
    private final CourseMilestones milestones = new CourseMilestones(content, counts);

    private static LearningContentClient.PracticeTestMilestone milestone() {
        return mock(LearningContentClient.PracticeTestMilestone.class);
    }

    @Test
    void aRequestReadsTheCountFromContentAndKeepsIt() {
        when(content.getPracticeTestMilestones(course)).thenReturn(List.of(milestone(), milestone()));

        assertEquals(2, milestones.countForRequest(course));

        verify(counts).saveIfChanged(course, 2);
    }

    @Test
    void aCourseWithoutAKeptCountAsksContentOnceToKeepOne() {
        when(counts.find(course)).thenReturn(Optional.empty());
        when(content.getPracticeTestMilestones(course)).thenReturn(List.of(milestone()));

        milestones.ensureKnown(course);

        verify(counts).saveIfChanged(course, 1);
    }

    @Test
    void aCourseWithAKeptCountDoesNotCallContentAgain() {
        when(counts.find(course)).thenReturn(Optional.of(3));

        milestones.ensureKnown(course);

        verifyNoInteractions(content);
        verify(counts, never()).saveIfChanged(any(), anyInt());
    }

    @Test
    void anEventReadsTheKeptCountAndNeverCallsContent() {
        when(counts.find(course)).thenReturn(Optional.of(3));

        assertEquals(3, milestones.countForEvent(course));

        verifyNoInteractions(content);
    }

    @Test
    void anEventWithoutAKeptCountFailsSoItIsTriedAgainOnceARequestHasKeptOne() {
        when(counts.find(course)).thenReturn(Optional.empty());

        var failure = assertThrows(LearningRequestException.class, () -> milestones.countForEvent(course));

        assertEquals("MILESTONE_COUNT_UNKNOWN", failure.getCode());
        verifyNoInteractions(content);
    }
}

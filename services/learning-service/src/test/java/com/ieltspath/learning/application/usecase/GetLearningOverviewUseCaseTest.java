package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningOverviewReader;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GetLearningOverviewUseCaseTest {
    @Test
    void convertsTheLifetimeSumToSecondsOnceAndKeepsTrackingAvailability() {
        UUID user = UUID.randomUUID();
        Instant started = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-11-08T05:00:00Z");
        var reader = mock(LearningOverviewReader.class);
        when(reader.totals(user)).thenReturn(new LearningOverviewReader.Totals(started, 1999, 8));

        var result = new GetLearningOverviewUseCase(reader, Clock.fixed(now, ZoneOffset.UTC)).execute(user);

        assertEquals(now, result.generatedAt());
        assertEquals(started, result.dataFrom());
        assertEquals(1, result.totalStudySeconds());
        assertEquals(8, result.completedLessons());
        verify(reader).totals(user);
        verifyNoMoreInteractions(reader);
    }
}

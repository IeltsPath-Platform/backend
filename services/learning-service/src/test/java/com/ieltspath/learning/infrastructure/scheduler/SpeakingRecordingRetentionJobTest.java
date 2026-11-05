package com.ieltspath.learning.infrastructure.scheduler;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.service.SpeakingSettingsFixture;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeakingRecordingRetentionJobTest {
    private static final Instant NOW = Instant.parse("2026-11-04T05:00:00Z");

    private final SpeakingSubmissionRepository submissions = mock(SpeakingSubmissionRepository.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private final SpeakingRecordingRetentionJob job = new SpeakingRecordingRetentionJob(submissions, storage,
            SpeakingSettingsFixture.enabled());

    private static List<SpeakingSubmission> expired(int count) {
        List<SpeakingSubmission> list = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            list.add(SpeakingSubmission.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    SpeakingScope.PART, 5, NOW, List.of(SpeakingRecording.expected(UUID.randomUUID(), UUID.randomUUID(), 1, 1)), NOW));
        }
        return list;
    }

    @Test
    void aRunKeepsGoingUntilNothingExpiredIsLeft() {
        when(submissions.findExpiredBefore(any(), anyInt())).thenReturn(expired(100), expired(100), expired(30), List.of());
        when(submissions.deleteExpired(any(), any())).thenReturn(true);

        job.purgeBatch();

        verify(submissions, times(230)).deleteExpired(any(), any());
    }

    @Test
    void aRunStopsWhenNoRowCouldBePurged() {
        when(submissions.findExpiredBefore(any(), anyInt())).thenReturn(expired(3));
        when(submissions.deleteExpired(any(), any())).thenReturn(false);

        job.purgeBatch();

        // One read, not fifty: the same stuck rows would come back every time.
        verify(submissions, times(1)).findExpiredBefore(any(), anyInt());
    }
}

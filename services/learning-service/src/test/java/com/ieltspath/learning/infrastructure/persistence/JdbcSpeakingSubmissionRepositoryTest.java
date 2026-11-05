package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "learning.league.close-enabled=false",
        "learning.rewards.relay-enabled=false", "app.auth.internal-jwt-issuer=urn:test:gateway",
        "app.auth.internal-jwt-secret=dGVzdC1vbmx5LWludGVybmFsLWp3dC1zaWduaW5nLWtleQ=="})
class JdbcSpeakingSubmissionRepositoryTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired SpeakingSubmissionRepository repository;

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static SpeakingSubmission submission(UUID userId, UUID requestId, UUID... questions) {
        List<SpeakingRecording> expected = new java.util.ArrayList<>();
        for (int index = 0; index < questions.length; index++) {
            expected.add(SpeakingRecording.expected(questions[index], UUID.randomUUID(), 1, index + 1));
        }
        return SpeakingSubmission.start(UUID.randomUUID(), userId, requestId, UUID.randomUUID(), SpeakingScope.PART, 5,
                NOW, expected, NOW);
    }

    @Test
    void savesASubmissionWithItsEmptyRecordingsAndReadsItBack() {
        UUID user = UUID.randomUUID();
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        SpeakingSubmission saved = submission(user, UUID.randomUUID(), q1, q2);
        repository.save(saved);

        SpeakingSubmission found = repository.findOwned(saved.id(), user).orElseThrow();

        assertFalse(saved.isNew());
        assertEquals(SpeakingSubmissionStatus.UPLOADING, found.status());
        assertEquals(SpeakingScope.PART, found.scope());
        assertEquals(5, found.pointCost());
        assertEquals(List.of(q1, q2), found.recordings().stream().map(SpeakingRecording::questionVersionId).toList());
        assertTrue(found.recordings().stream().noneMatch(SpeakingRecording::isUploaded));
        assertEquals(saved.requestId(), repository.findByRequestId(saved.requestId()).orElseThrow().requestId());
    }

    @Test
    void aSubmissionIsVisibleOnlyToItsOwner() {
        SpeakingSubmission saved = submission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        repository.save(saved);

        assertTrue(repository.findOwned(saved.id(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void savesUploadedRecordingsAndTheQueuedState() {
        UUID user = UUID.randomUUID();
        UUID q1 = UUID.randomUUID();
        SpeakingSubmission saved = submission(user, UUID.randomUUID(), q1);
        repository.save(saved);

        SpeakingSubmission loaded = repository.findForUpdate(saved.id()).orElseThrow();
        loaded.attachRecording(q1, 1, "practice/" + user + "/2026-11-04_" + saved.id() + "/part1_q01.wav", "audio/wav",
                32000, 4000, NOW);
        loaded.complete(NOW.plusSeconds(1));
        repository.save(loaded);

        SpeakingSubmission found = repository.findOwned(saved.id(), user).orElseThrow();
        SpeakingRecording recording = found.recordings().get(0);
        assertEquals(SpeakingSubmissionStatus.QUEUED, found.status());
        assertEquals(NOW.plusSeconds(1), found.queuedAt());
        assertTrue(recording.isUploaded());
        assertEquals("audio/wav", recording.contentType());
        assertEquals(32000, recording.sizeBytes());
        assertEquals(4000, recording.durationMs());
        assertEquals(1, recording.attempt());
    }

    @Test
    void aRecordingWithoutDurationReadsBackAsNull() {
        UUID user = UUID.randomUUID();
        UUID q1 = UUID.randomUUID();
        SpeakingSubmission saved = submission(user, UUID.randomUUID(), q1);
        repository.save(saved);
        SpeakingSubmission loaded = repository.findForUpdate(saved.id()).orElseThrow();
        loaded.attachRecording(q1, 1, "practice/x/part1_q01.wav", "audio/wav", 10, null, NOW);
        repository.save(loaded);

        assertNull(repository.findOwned(saved.id(), user).orElseThrow().recordings().get(0).durationMs());
    }

    @Test
    void findsOnlyTheSubmissionsStillWaiting() {
        UUID user = UUID.randomUUID();
        SpeakingSubmission abandoned = submission(user, UUID.randomUUID(), UUID.randomUUID());
        repository.save(abandoned);
        abandoned.abandon(NOW);
        repository.save(abandoned);
        SpeakingSubmission waiting = submission(user, UUID.randomUUID(), UUID.randomUUID());
        repository.save(waiting);

        List<SpeakingSubmission> active = repository.findActive(user);

        assertEquals(List.of(waiting.id()), active.stream().map(SpeakingSubmission::id).toList());
    }

    @Test
    void theDatabaseRefusesASecondWaitingSubmissionOfTheSameLearner() {
        UUID user = UUID.randomUUID();
        repository.save(submission(user, UUID.randomUUID(), UUID.randomUUID()));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(submission(user, UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void theDatabaseRefusesTheSameRequestIdTwice() {
        UUID requestId = UUID.randomUUID();
        repository.save(submission(UUID.randomUUID(), requestId, UUID.randomUUID()));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(submission(UUID.randomUUID(), requestId, UUID.randomUUID())));
    }

    @Test
    void keepsTheFirstGradingContextSavedForASubmission() {
        SpeakingSubmission saved = submission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        repository.save(saved);
        assertTrue(repository.findGradingContext(saved.id()).isEmpty());

        repository.saveGradingContext(saved.id(), "{\"errorTypes\":[]}");
        repository.saveGradingContext(saved.id(), "{\"errorTypes\":[1]}");

        // jsonb prints a space after the colon.
        assertEquals("{\"errorTypes\": []}", repository.findGradingContext(saved.id()).orElseThrow());
    }
}

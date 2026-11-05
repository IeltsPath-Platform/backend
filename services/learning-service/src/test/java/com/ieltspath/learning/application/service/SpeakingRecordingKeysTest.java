package com.ieltspath.learning.application.service;

import com.ieltspath.commonstorage.key.ObjectKeys;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeakingRecordingKeysTest {
    private static final UUID USER = UUID.fromString("0b1f3c52-8a6e-4d0c-9b0e-3f1f2a6c7d10");
    private static final UUID SUBMISSION = UUID.fromString("9c2e5d77-1b3a-4f68-8e21-5a7c9d0e4b33");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 4);

    @Test
    void readsAsUserThenDayAndSubmissionThenPartAndQuestion() {
        assertEquals("practice/0b1f3c52-8a6e-4d0c-9b0e-3f1f2a6c7d10/2026-11-04_9c2e5d77-1b3a-4f68-8e21-5a7c9d0e4b33/part1_q01.wav",
                SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 1, 1, 1, "wav"));
        assertEquals("practice/0b1f3c52-8a6e-4d0c-9b0e-3f1f2a6c7d10/2026-11-04_9c2e5d77-1b3a-4f68-8e21-5a7c9d0e4b33/part2_q12.wav",
                SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 2, 12, 1, "wav"));
    }

    @Test
    void aQuestionRecordedAgainGetsASuffixInsteadOfTheSameKey() {
        assertTrue(SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 3, 1, 2, "wav").endsWith("/part3_q01_r2.wav"));
        assertTrue(SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 3, 1, 3, "webm").endsWith("/part3_q01_r3.webm"));
    }

    @Test
    void aStoredFileGetsARandomPartSoALeftoverFileNeverBlocksTheNextAttempt() {
        String first = SpeakingRecordingKeys.uniqueKey(USER, DAY, SUBMISSION, 1, 1, 2, "wav");
        String second = SpeakingRecordingKeys.uniqueKey(USER, DAY, SUBMISSION, 1, 1, 2, "wav");

        assertTrue(first.matches(".*/part1_q01_r2_[0-9a-f]{8}\\.wav"), first);
        assertFalse(first.equals(second));
        assertTrue(SpeakingRecordingKeys.isOwnedBy(first, USER));
        ObjectKeys.requireSafe(first);
    }

    @Test
    void everyKeyPassesTheStorageSafetyCheck() {
        ObjectKeys.requireSafe(SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 1, 1, 1, "wav"));
        ObjectKeys.requireSafe(SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 2, 5, 4, "webm"));
    }

    @Test
    void keysBelongToTheirLearnerOnly() {
        String key = SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 1, 1, 1, "wav");

        assertTrue(SpeakingRecordingKeys.isOwnedBy(key, USER));
        assertFalse(SpeakingRecordingKeys.isOwnedBy(key, UUID.randomUUID()));
        assertFalse(SpeakingRecordingKeys.isOwnedBy("speaking/" + USER + "/x.webm", USER));
        assertFalse(SpeakingRecordingKeys.isOwnedBy(null, USER));
    }

    @Test
    void positionsStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 0, 1, 1, "wav"));
        assertThrows(IllegalArgumentException.class, () -> SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 1, 0, 1, "wav"));
        assertThrows(IllegalArgumentException.class, () -> SpeakingRecordingKeys.key(USER, DAY, SUBMISSION, 1, 1, 0, "wav"));
    }
}

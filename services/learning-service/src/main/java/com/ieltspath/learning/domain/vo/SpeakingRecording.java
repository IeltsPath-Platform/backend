package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.UUID;

/**
 * The recording of one question of a submission. It exists from the moment the submission is created, empty
 * ({@code objectKey} null), and is filled when the file is stored. {@code attempt} counts the files stored for the
 * question so far; storing again uses the next number in the object key.
 */
public record SpeakingRecording(UUID questionVersionId, UUID sectionId, int partNo, int questionNo, int attempt,
                                String objectKey, String contentType, long sizeBytes, Integer durationMs,
                                Instant uploadedAt, String questionText) {

    public static SpeakingRecording expected(UUID questionVersionId, UUID sectionId, int partNo, int questionNo) {
        return expected(questionVersionId, sectionId, partNo, questionNo, "");
    }

    public static SpeakingRecording expected(UUID questionVersionId, UUID sectionId, int partNo, int questionNo,
                                             String questionText) {
        return new SpeakingRecording(questionVersionId, sectionId, partNo, questionNo, 0, null, null, 0, null, null,
                questionText);
    }

    public SpeakingRecording(UUID questionVersionId, UUID sectionId, int partNo, int questionNo, int attempt,
                             String objectKey, String contentType, long sizeBytes, Integer durationMs,
                             Instant uploadedAt) {
        this(questionVersionId, sectionId, partNo, questionNo, attempt, objectKey, contentType, sizeBytes,
                durationMs, uploadedAt, "");
    }

    public boolean isUploaded() {
        return objectKey != null;
    }

    public SpeakingRecording uploaded(int newAttempt, String key, String type, long size, Integer duration, Instant at) {
        return new SpeakingRecording(questionVersionId, sectionId, partNo, questionNo, newAttempt, key, type, size,
                duration, at, questionText);
    }
}

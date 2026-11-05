package com.ieltspath.learning.application.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Where a Speaking recording lives in the bucket:
 * {@code practice/{userId}/{yyyy-MM-dd}_{submissionId}/part{n}_q{nn}[_r{k}].{ext}}. The user, the day and the submission
 * are readable from the folders, the part and the question from the file name. The first file of a question has no
 * suffix; storing it again uses {@code _r2}, {@code _r3}, because the storage never overwrites a key.
 */
public final class SpeakingRecordingKeys {
    public static final String ROOT = "practice";

    private SpeakingRecordingKeys() {
    }

    /** {@code attempt} is 1 for the first file of the question. */
    public static String key(UUID userId, LocalDate day, UUID submissionId, int partNo, int questionNo, int attempt,
                             String extension) {
        return build(userId, day, submissionId, partNo, questionNo, attempt, "", extension);
    }

    /**
     * The key a stored file gets: {@link #key} plus a random part. The storage never overwrites, so without it a file
     * left behind by an upload whose record could not be saved would block every later attempt for the same question.
     */
    public static String uniqueKey(UUID userId, LocalDate day, UUID submissionId, int partNo, int questionNo, int attempt,
                                   String extension) {
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return build(userId, day, submissionId, partNo, questionNo, attempt, "_" + nonce, extension);
    }

    private static String build(UUID userId, LocalDate day, UUID submissionId, int partNo, int questionNo, int attempt,
                                String nonceSuffix, String extension) {
        if (partNo < 1 || questionNo < 1 || attempt < 1) throw new IllegalArgumentException("positions start at 1");
        String suffix = attempt == 1 ? "" : "_r" + attempt;
        return userPrefix(userId) + day + "_" + submissionId.toString().toLowerCase(Locale.ROOT) + "/part" + partNo
                + "_q" + String.format("%02d", questionNo) + suffix + nonceSuffix + "." + extension;
    }

    /** The folder holding every recording of the learner. */
    public static String userPrefix(UUID userId) {
        return ROOT + "/" + userId.toString().toLowerCase(Locale.ROOT) + "/";
    }

    public static boolean isOwnedBy(String key, UUID userId) {
        return key != null && key.startsWith(userPrefix(userId));
    }
}

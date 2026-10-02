package com.group01.learning.application.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class LessonEvidenceReference {
    private static final UUID DNS_NAMESPACE = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    private static final UUID LESSON_NAMESPACE = uuid5(DNS_NAMESPACE, "ielts-path:lesson_exercise");
    private static final UUID REVIEW_NAMESPACE = uuid5(DNS_NAMESPACE, "ielts-path:review_set");
    private static final UUID WRITING_NAMESPACE = uuid5(DNS_NAMESPACE, "ielts-path:lesson_writing");
    /** Fixed by the AssessmentCompleted.v2 contract. */
    private static final UUID ASSESSMENT_NAMESPACE = UUID.fromString("6f0c1b2e-3d7a-4e59-9b8a-2c4d5e6f7a81");

    private LessonEvidenceReference() {}

    public static UUID create(UUID requestId, UUID questionVersionId, UUID kpId) {
        return uuid5(LESSON_NAMESPACE, requestId + ":" + questionVersionId + ":" + kpId);
    }

    /** Reference of review-set evidence; a separate namespace keeps it apart from lesson evidence. */
    public static UUID forReviewSet(UUID requestId, UUID questionVersionId, UUID kpId) {
        return uuid5(REVIEW_NAMESPACE, requestId + ":" + questionVersionId + ":" + kpId);
    }

    /** Reference of essay evidence: one per graded submission and knowledge point. */
    public static UUID forWriting(UUID submissionId, UUID kpId) {
        return uuid5(WRITING_NAMESPACE, submissionId + ":" + kpId);
    }

    /** Reference of formal-result evidence: one per result version, item and knowledge point. */
    public static UUID forAssessment(UUID resultId, int resultVersion, UUID itemResultId, UUID kpId) {
        return uuid5(ASSESSMENT_NAMESPACE, resultId + ":" + resultVersion + ":" + itemResultId + ":" + kpId);
    }

    private static UUID uuid5(UUID namespace, String name) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(ByteBuffer.allocate(16).putLong(namespace.getMostSignificantBits())
                    .putLong(namespace.getLeastSignificantBits()).array());
            byte[] bytes = digest.digest(name.getBytes(StandardCharsets.UTF_8));
            bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x50);
            bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is required for UUIDv5", exception);
        }
    }
}

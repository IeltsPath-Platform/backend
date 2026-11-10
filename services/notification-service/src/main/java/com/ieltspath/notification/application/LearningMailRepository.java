package com.ieltspath.notification.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contacts, mail switches and the examiner job digest that the learning mails read and write. */
public interface LearningMailRepository {
    /** Stores the address and name only if the report is newer than the one already held. */
    void upsertContact(UUID userId, String email, String displayName, Instant occurredAt);

    Optional<Contact> findContact(UUID userId);

    /** The learning category is on unless the user switched it off. */
    boolean learningEnabled(UUID userId);

    void setLearningEnabled(UUID userId, boolean enabled, Instant changedAt);

    void addPendingExaminerJob(UUID gradingJobId, UUID examinerId, Instant createdAt);

    List<UUID> examinersWithPendingJobs();

    int pendingExaminerJobCount(UUID examinerId);

    void clearPendingExaminerJobs(UUID examinerId);

    record Contact(String email, String displayName) { }
}

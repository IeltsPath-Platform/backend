package com.ieltspath.user.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads and keeps the examiners learners can choose from. Only active users with the examiner role count. */
public interface ExaminerDirectoryReader {
    /** The active examiners who currently accept submissions, ordered by name. */
    List<Examiner> findAccepting();

    /** An active examiner, accepting submissions or not. */
    Optional<Examiner> findActive(UUID userId);

    void saveProfile(UUID userId, BigDecimal ieltsBand, String certificatesJson, boolean acceptingSubmissions);

    /** @return false when the user is not an active examiner */
    boolean saveCapacity(UUID userId, int dailyLimit);

    record Examiner(UUID id, String displayName, String avatarReference, String bio, BigDecimal ieltsBand,
                    String certificatesJson, boolean acceptingSubmissions, int dailyLimit) {}
}

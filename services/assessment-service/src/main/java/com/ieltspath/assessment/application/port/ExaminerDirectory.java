package com.ieltspath.assessment.application.port;

import java.util.Optional;
import java.util.UUID;

/** What assessment needs to know about an examiner from the user service. */
public interface ExaminerDirectory {
    /**
     * @return the examiner, or empty when the id is not an active examiner. A failure to reach the user service is
     * thrown as {@link com.ieltspath.assessment.application.exception.UserServiceUnavailableException}.
     */
    Optional<Examiner> find(UUID examinerId);

    record Examiner(UUID id, boolean acceptingSubmissions, int dailyLimit) {}
}

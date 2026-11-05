package com.ieltspath.assessment.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** What assessment needs to know about an examiner from the user service. */
public interface ExaminerDirectory {
    /**
     * @return the examiner, or empty when the id is not an active examiner. A failure to reach the user service is
     * thrown as {@link com.ieltspath.assessment.application.exception.UserServiceUnavailableException}.
     */
    Optional<Examiner> find(UUID examinerId);

    /**
     * Several examiners in one call, for a list: never ask {@link #find} once per examiner. Ids that are not active examiners
     * are absent from the answer. A failure to reach the user service is thrown as
     * {@link com.ieltspath.assessment.application.exception.UserServiceUnavailableException}.
     */
    Map<UUID, Examiner> findAll(Collection<UUID> examinerIds);

    record Examiner(UUID id, boolean acceptingSubmissions, int dailyLimit) {}
}

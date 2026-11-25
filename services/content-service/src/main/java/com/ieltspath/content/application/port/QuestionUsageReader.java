package com.ieltspath.content.application.port;

import java.util.List;
import java.util.UUID;

/** Where the versions of a question are used: in the sections of exam packages and in the exercise blocks of lessons. */
public interface QuestionUsageReader {

    List<Usage> usagesOf(UUID questionId);

    enum Kind { PACKAGE, LESSON }

    /**
     * @param targetId the package or lesson id
     * @param location the section title of a package, or the position of the exercise block in a lesson
     * @param targetVersionNumber the package version holding the question; null for a lesson
     * @param targetStatus status of the package version, or of the lesson
     */
    record Usage(Kind kind, UUID targetId, String targetCode, String targetTitle, String location,
                 Integer targetVersionNumber, String targetStatus, UUID questionVersionId, int questionVersionNumber) {
    }
}

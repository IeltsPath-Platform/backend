package com.group01.assessment.domain.vo;

import com.group01.assessment.domain.exception.PackageNotAttemptableException;

public enum AttemptType {
    PLACEMENT, OFFICIAL_PRACTICE, MOCK, TOPIC_GATE, QUIZ, COURSE_GATE;

    /**
     * The attempt type is decided by the Content package, never by the client, so a final-test code cannot be
     * taken as a practice attempt. Practice sets are graded by Learning Service and lessons are not tests.
     */
    public static AttemptType forContentPackageType(String packageType) {
        if (packageType == null) {
            throw new PackageNotAttemptableException("Package type is missing");
        }
        return switch (packageType) {
            case "TOPIC_TEST" -> TOPIC_GATE;
            case "COURSE_TEST" -> COURSE_GATE;
            case "MOCK_TEST" -> MOCK;
            case "PLACEMENT_TEST" -> PLACEMENT;
            case "QUIZ" -> QUIZ;
            default -> throw new PackageNotAttemptableException("Packages of type " + packageType
                    + " cannot be taken as an assessment attempt");
        };
    }
}

package com.ieltspath.assessment.domain.vo;

import com.ieltspath.assessment.domain.exception.PackageNotAttemptableException;

public enum AttemptType {
    PLACEMENT, OFFICIAL_PRACTICE, MOCK, TOPIC_GATE, QUIZ, COURSE_GATE;

    /**
     * The attempt type is decided by the Content package, never by the client, so a final-test code cannot be
     * taken as a practice attempt. Practice sets are graded by Learning Service and lessons are not tests.
     */
    public static AttemptType forContentPackageType(String packageType) {
        if (packageType == null) {
            throw new PackageNotAttemptableException("Thiếu thông tin loại gói bài thi.");
        }
        return switch (packageType) {
            case "TOPIC_TEST" -> TOPIC_GATE;
            case "COURSE_TEST" -> COURSE_GATE;
            case "MOCK_TEST" -> MOCK;
            case "PRACTICE_TEST" -> OFFICIAL_PRACTICE;
            case "PLACEMENT_TEST" -> PLACEMENT;
            case "QUIZ" -> QUIZ;
            default -> throw new PackageNotAttemptableException("Loại gói đề này không thể dùng để làm bài đánh giá.");
        };
    }
}

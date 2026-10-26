package com.ieltspath.user.api.dto.response;

import com.ieltspath.user.application.result.ExaminerResult;

import java.util.UUID;

/** What assessment asks about an examiner before giving them a job. */
public record ExaminerAvailabilityResponse(UUID id, boolean acceptingSubmissions, int dailyLimit) {
    public static ExaminerAvailabilityResponse from(ExaminerResult result) {
        return new ExaminerAvailabilityResponse(result.id(), result.acceptingSubmissions(), result.dailyLimit());
    }
}

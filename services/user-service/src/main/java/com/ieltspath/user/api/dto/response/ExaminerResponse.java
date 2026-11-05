package com.ieltspath.user.api.dto.response;

import com.ieltspath.user.application.result.ExaminerResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** What a learner sees of an examiner: who they are and whether they take new submissions. The workload limit stays inside. */
public record ExaminerResponse(UUID id, String displayName, String avatarUrl, String bio, BigDecimal ieltsBand,
                               List<ExaminerResult.Certificate> certificates, boolean acceptingSubmissions) {
    public static ExaminerResponse from(ExaminerResult result) {
        return new ExaminerResponse(result.id(), result.displayName(), result.avatarUrl(), result.bio(),
                result.ieltsBand(), result.certificates(), result.acceptingSubmissions());
    }
}

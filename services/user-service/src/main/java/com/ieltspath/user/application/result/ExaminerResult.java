package com.ieltspath.user.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** An examiner as a learner sees them when choosing, plus the capacity assessment needs. */
public record ExaminerResult(UUID id, String displayName, String avatarUrl, String bio, BigDecimal ieltsBand,
                             List<Certificate> certificates, boolean acceptingSubmissions, int dailyLimit) {
    public record Certificate(String name, String issuer, Integer year, String score) {}
}

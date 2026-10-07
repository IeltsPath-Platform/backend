package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.TestAssignmentResult;

import java.util.UUID;

public record TestAssignmentResponse(UUID assignmentId, UUID packageId, UUID packageVersionId) {
    public static TestAssignmentResponse from(TestAssignmentResult result) {
        return new TestAssignmentResponse(result.assignmentId(), result.packageId(), result.packageVersionId());
    }
}

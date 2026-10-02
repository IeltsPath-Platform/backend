package com.group01.learning.api.dto.response;

import com.group01.learning.application.result.TestAssignmentResult;

import java.util.UUID;

public record TestAssignmentResponse(UUID assignmentId, UUID packageId, UUID packageVersionId) {
    public static TestAssignmentResponse from(TestAssignmentResult result) {
        return new TestAssignmentResponse(result.assignmentId(), result.packageId(), result.packageVersionId());
    }
}

package com.ieltspath.assessment.application.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.ieltspath.assessment.application.service.ExaminerAssignmentPolicy.State.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExaminerAssignmentPolicyTest {
    private final Instant now = Instant.parse("2026-10-22T10:00:00Z");

    @Test
    void aJobIsLateExactlyWhenTheDeadlineHasPassed() {
        assertFalse(ExaminerAssignmentPolicy.isOverdue(now.minusSeconds(48 * 3600 - 1), now));
        assertTrue(ExaminerAssignmentPolicy.isOverdue(now.minusSeconds(48 * 3600), now));
        assertFalse(ExaminerAssignmentPolicy.isOverdue(null, now));
    }

    @Test
    void theStateFollowsTheStatusAndTheChoice() {
        assertEquals(AWAITING_CHOICE, ExaminerAssignmentPolicy.stateOf("QUEUED", false, null, now));
        assertEquals(WAITING, ExaminerAssignmentPolicy.stateOf("QUEUED", true, now.minusSeconds(3600), now));
        assertEquals(WAITING, ExaminerAssignmentPolicy.stateOf("PROCESSING", true, now.minusSeconds(3600), now));
        assertEquals(OVERDUE, ExaminerAssignmentPolicy.stateOf("QUEUED", true, now.minusSeconds(49 * 3600), now));
        assertEquals(COMPLETED, ExaminerAssignmentPolicy.stateOf("COMPLETED", true, now.minusSeconds(49 * 3600), now));
        assertEquals(CLOSED, ExaminerAssignmentPolicy.stateOf("CANCELLED", true, now, now));
        assertEquals(CLOSED, ExaminerAssignmentPolicy.stateOf("FAILED", false, null, now));
    }

    @Test
    void theLearnerMayChooseWhenNobodyWasChosenOrAfterTheDeadlineWithinTheLimit() {
        assertTrue(ExaminerAssignmentPolicy.canChoose(AWAITING_CHOICE, 0));
        assertTrue(ExaminerAssignmentPolicy.canChoose(OVERDUE, 0));
        assertTrue(ExaminerAssignmentPolicy.canChoose(OVERDUE, ExaminerAssignmentPolicy.MAX_CHANGES - 1));
        assertFalse(ExaminerAssignmentPolicy.canChoose(OVERDUE, ExaminerAssignmentPolicy.MAX_CHANGES));
        assertFalse(ExaminerAssignmentPolicy.canChoose(WAITING, 0));
        assertFalse(ExaminerAssignmentPolicy.canChoose(COMPLETED, 0));
    }
}

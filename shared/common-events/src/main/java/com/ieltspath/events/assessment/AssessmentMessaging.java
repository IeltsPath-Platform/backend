package com.ieltspath.events.assessment;

/** Assessment publishes results consumed by Learning; see docs/contracts/assessment-completed-v2.md. */
public final class AssessmentMessaging {
    public static final String EXCHANGE = "assessment.events";
    public static final String COMPLETED_ROUTING_KEY = "assessment.completed.v2";
    /** The chosen examiner did not grade in time; consumed by Notification. */
    public static final String GRADING_OVERDUE_ROUTING_KEY = GradingOverdueV1.EVENT_TYPE;
    /** A job the examiner was chosen for will be late soon; consumed by Notification. */
    public static final String GRADING_DUE_SOON_ROUTING_KEY = GradingDueSoonV1.EVENT_TYPE;
    /** A learner chose the examiner for a job: the examiner is told at once; consumed by Notification. */
    public static final String GRADING_REQUESTED_ROUTING_KEY = GradingRequestedV1.EVENT_TYPE;
    /** The examiner finished grading a learner's essay: the learner is told; consumed by Notification. */
    public static final String GRADING_COMPLETED_ROUTING_KEY = GradingCompletedV1.EVENT_TYPE;

    private AssessmentMessaging() { }
}

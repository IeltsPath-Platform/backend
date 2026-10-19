package com.ieltspath.events.assessment;

/** Assessment publishes results consumed by Learning; see docs/contracts/assessment-completed-v2.md. */
public final class AssessmentMessaging {
    public static final String EXCHANGE = "assessment.events";
    public static final String COMPLETED_ROUTING_KEY = "assessment.completed.v2";

    private AssessmentMessaging() { }
}

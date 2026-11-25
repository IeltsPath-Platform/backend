package com.ieltspath.assessment.application.port;

/** Reads whether the current learner has active Premium access. */
public interface LearnerEntitlement {
    boolean premium();
}

package com.ieltspath.events.library;

/** Library publishes learner reminders consumed by Notification; see docs/contracts/vocabulary-review-due-v1.md. */
public final class LibraryMessaging {
    public static final String EXCHANGE = "library.events";
    /** A learner has saved words that are due for review today; consumed by Notification. */
    public static final String VOCABULARY_REVIEW_DUE_ROUTING_KEY = VocabularyReviewDueV1.EVENT_TYPE;

    private LibraryMessaging() { }
}

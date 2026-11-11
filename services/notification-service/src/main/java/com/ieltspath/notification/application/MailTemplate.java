package com.ieltspath.notification.application;

import java.util.Set;

/**
 * Every email the service can send. One constant = one template folder under {@code mail-templates/} plus the variables
 * its body and subject require. A render with a missing variable is refused, so a template cannot ship half-filled.
 */
public enum MailTemplate {
    PASSWORD_RESET("password-reset", Set.of("displayName", "resetLink", "expiresInMinutes")),
    PASSWORD_CHANGED("password-changed", Set.of("displayName", "changedAt")),
    GOOGLE_ACCOUNT_LINKED("google-account-linked", Set.of("displayName")),
    EMAIL_VERIFICATION("email-verification", Set.of("displayName", "verifyLink", "expiresInMinutes")),
    SUBSCRIPTION_EXPIRING("subscription-expiring", Set.of("displayName", "endsAt")),
    SUBSCRIPTION_EXPIRED("subscription-expired", Set.of("displayName", "endsAt")),
    ESSAY_GRADED("essay-graded", Set.of("displayName", "actionLink", "unsubscribeLink")),
    GRADING_OVERDUE("grading-overdue", Set.of("displayName", "actionLink", "unsubscribeLink")),
    RESULT_READY("result-ready", Set.of("displayName", "actionLink", "unsubscribeLink")),
    EXAMINER_DUE_SOON("examiner-due-soon", Set.of("displayName", "actionLink", "unsubscribeLink")),
    EXAMINER_NEW_JOBS("examiner-new-jobs", Set.of("displayName", "jobCount", "actionLink", "unsubscribeLink")),
    SUBSCRIPTION_GRANTED("subscription-granted", Set.of("displayName", "endsAt"));

    private final String key;
    private final Set<String> variables;

    MailTemplate(String key, Set<String> variables) {
        this.key = key;
        this.variables = variables;
    }

    /** Folder name under {@code mail-templates/}, and the key used in configuration and in the delivery log. */
    public String key() {
        return key;
    }

    /** Variables the model must provide, non-null. */
    public Set<String> variables() {
        return variables;
    }
}

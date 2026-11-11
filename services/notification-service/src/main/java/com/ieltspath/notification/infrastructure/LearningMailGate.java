package com.ieltspath.notification.infrastructure;

/**
 * Condition shared by the learning-mail beans. Learning mails need an unsubscribe secret on top of the usual mail settings,
 * so an environment without it does not send mails whose opt-out link cannot work.
 */
public final class LearningMailGate {
    public static final String EXPRESSION = "${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and "
            + "'${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != '' and "
            + "'${notification.mail.unsubscribe-secret:}' != ''";

    private LearningMailGate() { }
}

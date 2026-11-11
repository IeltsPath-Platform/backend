package com.ieltspath.notification.application;

/** Port for delivering one rendered email to one address. */
public interface MailSender {
    void send(String to, RenderedMail mail);
}

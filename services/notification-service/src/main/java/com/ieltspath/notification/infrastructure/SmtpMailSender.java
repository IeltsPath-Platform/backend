package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.MailSender;
import com.ieltspath.notification.application.RenderedMail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Sends a rendered mail over SMTP as multipart/alternative: plain text first, then HTML. */
@Component
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != ''")
public class SmtpMailSender implements MailSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpMailSender(JavaMailSender mailSender, @Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, RenderedMail mail) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(mail.subject());
            helper.setText(mail.text(), mail.html());
            mailSender.send(message);
        } catch (MessagingException | MailException failure) {
            throw new IllegalStateException("Mail delivery failed", failure);
        }
    }
}

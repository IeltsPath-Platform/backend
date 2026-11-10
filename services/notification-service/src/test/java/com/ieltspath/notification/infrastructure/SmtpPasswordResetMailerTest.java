package com.ieltspath.notification.infrastructure;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpPasswordResetMailerTest {
    @Test
    void sendsPlainAndEscapedHtmlMail() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        SmtpPasswordResetMailer mailer = new SmtpPasswordResetMailer(sender, "no-reply@example.test");
        mailer.send("learner@example.test", "<script>name</script>", "https://example.test/reset-password?token=abc");
        var message = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(message.capture());
        String mimeContent = textParts(message.getValue().getContent());
        assertTrue(mimeContent.contains("https://example.test/reset-password?token=abc"));
        assertTrue(mimeContent.contains("&lt;script&gt;name&lt;/script&gt;"));
    }

    private static String textParts(Object content) throws Exception {
        if (content instanceof jakarta.mail.Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                text.append(textParts(multipart.getBodyPart(index).getContent()));
            }
            return text.toString();
        }
        return content instanceof String text ? text : "";
    }
}

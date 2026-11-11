package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.RenderedMail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpMailSenderTest {
    @Test
    void sendsMultipartWithSubjectTextAndHtml() throws Exception {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        SmtpMailSender sender = new SmtpMailSender(javaMailSender, "no-reply@example.test");

        sender.send("learner@example.test", new RenderedMail("Tiêu đề", "<p>html body</p>", "plain body"));

        ArgumentCaptor<MimeMessage> captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captured.capture());
        MimeMessage message = captured.getValue();
        assertEquals("Tiêu đề", message.getSubject());
        String content = textParts(message.getContent());
        assertTrue(content.contains("plain body"));
        assertTrue(content.contains("<p>html body</p>"));
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

package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.PasswordResetMailer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
@ConditionalOnExpression("${notification.mail.enabled:false} and '${spring.mail.host:}' != '' and '${notification.mail.from:}' != '' and '${notification.mail.frontend-base-url:}' != ''")
public class SmtpPasswordResetMailer implements PasswordResetMailer {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordResetMailer(JavaMailSender mailSender,
                                   @org.springframework.beans.factory.annotation.Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender; this.from = from;
    }

    @Override
    public void send(String email, String displayName, String resetLink) {
        String safeName = displayName == null || displayName.isBlank() ? "bạn" : HtmlUtils.htmlEscape(displayName);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from); helper.setTo(email); helper.setSubject("Đặt lại mật khẩu IELTSPath");
            helper.setText("Xin chào " + (displayName == null || displayName.isBlank() ? "bạn" : displayName)
                    + ",\n\nDùng liên kết sau để đặt lại mật khẩu trong vòng 15 phút:\n" + resetLink
                    + "\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.",
                    "<p>Xin chào " + safeName + ",</p><p>Dùng liên kết sau để đặt lại mật khẩu trong vòng 15 phút:</p>"
                            + "<p><a href=\"" + HtmlUtils.htmlEscape(resetLink) + "\">Đặt lại mật khẩu</a></p>"
                            + "<p>Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>");
            mailSender.send(message);
        } catch (MessagingException | MailException failure) {
            throw new IllegalStateException("Password reset email delivery failed", failure);
        }
    }
}

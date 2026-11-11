package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.MailTemplate;
import com.ieltspath.notification.application.RenderedMail;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThymeleafMailTemplateRendererTest {
    private final ThymeleafMailTemplateRenderer renderer = new ThymeleafMailTemplateRenderer();

    @Test
    void rendersSubjectHtmlAndTextForPasswordReset() {
        RenderedMail mail = renderer.render(MailTemplate.PASSWORD_RESET, model("Nguyễn Văn An"));

        assertEquals("Đặt lại mật khẩu IELTSPath", mail.subject());
        assertTrue(mail.text().contains("Xin chào Nguyễn Văn An,"));
        assertTrue(mail.text().contains("trong vòng 15 phút"));
        assertTrue(mail.text().contains("https://example.test/reset-password?token=abc"));
        assertTrue(mail.html().contains("href=\"https://example.test/reset-password?token=abc\""));
        assertTrue(mail.html().contains("IELTSPath"), "shared header from _layout must be included");
    }

    @Test
    void escapesHtmlVariablesButNotPlainText() {
        RenderedMail mail = renderer.render(MailTemplate.PASSWORD_RESET, model("<script>name</script>"));

        assertTrue(mail.html().contains("&lt;script&gt;name&lt;/script&gt;"));
        assertTrue(!mail.html().contains("<script>name</script>"));
        assertTrue(mail.text().contains("<script>name</script>"));
    }

    @Test
    void refusesMissingVariable() {
        Map<String, Object> incomplete = Map.of("displayName", "An", "resetLink", "https://example.test/x");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> renderer.render(MailTemplate.PASSWORD_RESET, incomplete));
        assertTrue(failure.getMessage().contains("expiresInMinutes"));
    }

    private static Map<String, Object> model(String name) {
        return Map.of("displayName", name, "resetLink", "https://example.test/reset-password?token=abc", "expiresInMinutes", 15L);
    }
}

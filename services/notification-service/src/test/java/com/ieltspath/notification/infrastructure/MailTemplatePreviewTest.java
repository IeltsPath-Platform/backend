package com.ieltspath.notification.infrastructure;

import com.ieltspath.notification.application.MailTemplate;
import com.ieltspath.notification.application.RenderedMail;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders every template with sample values and writes the results to {@code target/mail-previews} for review in a browser.
 * It also fails when a registered template cannot be rendered, so a broken template is caught before deploy.
 */
class MailTemplatePreviewTest {
    private static final String LINK = "https://learn.example.test/example?token=abc";
    private static final String UNSUBSCRIBE = "https://api.example.test/mail/unsubscribe?token=sample";

    @Test
    void everyTemplateRendersAndIsWrittenForPreview() throws IOException {
        ThymeleafMailTemplateRenderer renderer = new ThymeleafMailTemplateRenderer();
        Path out = Path.of("target", "mail-previews");
        Files.createDirectories(out);
        for (MailTemplate template : MailTemplate.values()) {
            RenderedMail mail = renderer.render(template, sampleModel(template));
            assertFalse(mail.subject().isBlank(), template.key());
            assertTrue(mail.html().contains("IELTSPath"), template.key() + " must include the shared header");
            assertFalse(mail.text().contains("th:"), template.key() + " must not leak template markup into text");
            Files.writeString(out.resolve(template.key() + ".html"), mail.html(), StandardCharsets.UTF_8);
            Files.writeString(out.resolve(template.key() + ".txt"),
                    mail.subject() + "\n\n" + mail.text(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, Object> sampleModel(MailTemplate template) {
        return switch (template) {
            case PASSWORD_RESET -> Map.of("displayName", "Nguyễn An", "resetLink", LINK, "expiresInMinutes", 15L);
            case PASSWORD_CHANGED -> Map.of("displayName", "Nguyễn An", "changedAt", "10:30 10/11/2026");
            case GOOGLE_ACCOUNT_LINKED -> Map.of("displayName", "Nguyễn An");
            case EMAIL_VERIFICATION -> Map.of("displayName", "Nguyễn An", "verifyLink", LINK, "expiresInMinutes", 60L);
            case SUBSCRIPTION_EXPIRING, SUBSCRIPTION_EXPIRED, SUBSCRIPTION_GRANTED ->
                    Map.of("displayName", "Nguyễn An", "endsAt", "10:30 10/12/2026");
            case ESSAY_GRADED, GRADING_OVERDUE, RESULT_READY, EXAMINER_DUE_SOON ->
                    Map.of("displayName", "Nguyễn An", "actionLink", LINK, "unsubscribeLink", UNSUBSCRIBE);
            case EXAMINER_NEW_JOBS ->
                    Map.of("displayName", "Cô Lan", "jobCount", 3, "actionLink", LINK, "unsubscribeLink", UNSUBSCRIBE);
        };
    }
}

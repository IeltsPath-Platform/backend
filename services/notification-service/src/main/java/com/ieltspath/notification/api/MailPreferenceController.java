package com.ieltspath.notification.api;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.notification.application.LearningMailRepository;
import com.ieltspath.notification.application.MailUnsubscribeTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** The learner's mail switches, and the public one-click unsubscribe link that changes one of them. */
@RestController
@RequiredArgsConstructor
public class MailPreferenceController {
    private static final String LEARNING = "learning";

    private final LearningMailRepository repository;
    private final MailUnsubscribeTokens tokens;
    private final CurrentUserProvider users;

    @GetMapping("/api/notifications/mail-preferences")
    public MailPreferences mailPreferences() {
        return new MailPreferences(repository.learningEnabled(currentUserId()));
    }

    @PutMapping("/api/notifications/mail-preferences")
    public MailPreferences updateMailPreferences(@Valid @RequestBody MailPreferences request) {
        repository.setLearningEnabled(currentUserId(), request.learning(), Instant.now());
        return request;
    }

    /** Public: the signed token names the account and category. Only the learning category can be switched off here. */
    @GetMapping(value = "/mail/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestParam("token") String token) {
        Instant now = Instant.now();
        var claim = tokens.verify(token, now);
        if (claim.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(page("Liên kết không hợp lệ hoặc đã hết hạn."));
        }
        if (LEARNING.equals(claim.get().category())) {
            repository.setLearningEnabled(claim.get().userId(), false, now);
        }
        return ResponseEntity.ok(page("Bạn đã tắt email học tập. Email về bảo mật và thanh toán vẫn được gửi."));
    }

    private UUID currentUserId() {
        return users.requireCurrentUser().id();
    }

    private static String page(String message) {
        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><title>IELTSPath</title></head>"
                + "<body style=\"font-family:Arial,sans-serif;max-width:560px;margin:48px auto;color:#1f2937\">"
                + "<h2 style=\"color:#0b1f5c\">IELTSPath</h2><p>" + message + "</p></body></html>";
    }

    public record MailPreferences(@NotNull Boolean learning) {
        public MailPreferences {
            learning = learning == null ? Boolean.TRUE : learning;
        }
    }
}

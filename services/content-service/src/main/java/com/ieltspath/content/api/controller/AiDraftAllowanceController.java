package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/content/admin/ai-drafts/allowance")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class AiDraftAllowanceController {
    private final AiUsageQuota quota;
    private final CurrentUserProvider currentUser;
    private final ContentAiProperties properties;
    private final Clock clock;
    public AiDraftAllowanceController(AiUsageQuota quota, CurrentUserProvider currentUser,
            ContentAiProperties properties, Clock clock) {
        this.quota = quota; this.currentUser = currentUser; this.properties = properties; this.clock = clock;
    }
    @GetMapping
    public Allowance get() {
        int limit = properties.getAi().getDailyLimit();
        int used = quota.used(currentUser.requireUserId(), LocalDate.now(clock), AiUsageKind.AI);
        return new Allowance(limit, used, Math.max(0, limit - used));
    }
    public record Allowance(int limit, int used, int remaining) {}
}

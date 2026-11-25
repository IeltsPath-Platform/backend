package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.port.QuestionUsageReader;
import com.ieltspath.content.application.port.QuestionUsageReader.Usage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Where the versions of one question are used, so an author sees which exams and lessons an edit would reach. */
@RestController
@RequestMapping("/api/content/admin/questions/{questionId}/usages")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
@RequiredArgsConstructor
public class QuestionUsageController {

    private final QuestionUsageReader usages;

    public record UsageResponse(String kind, UUID targetId, String targetCode, String targetTitle, String location,
                                Integer targetVersionNumber, String targetStatus, UUID questionVersionId,
                                int questionVersionNumber) {
        static UsageResponse from(Usage usage) {
            return new UsageResponse(usage.kind().name(), usage.targetId(), usage.targetCode(), usage.targetTitle(),
                    usage.location(), usage.targetVersionNumber(), usage.targetStatus(), usage.questionVersionId(),
                    usage.questionVersionNumber());
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UsageResponse> list(@PathVariable("questionId") UUID questionId) {
        return usages.usagesOf(questionId).stream().map(UsageResponse::from).toList();
    }
}

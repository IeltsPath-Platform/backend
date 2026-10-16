package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.result.PracticeHistoryResult;
import com.ieltspath.assessment.application.result.PracticeProgressResult;
import com.ieltspath.assessment.application.usecase.GetPracticeHistoryUseCase;
import com.ieltspath.assessment.application.usecase.GetPracticeProgressUseCase;
import com.ieltspath.assessment.domain.vo.AttemptScope;
import com.ieltspath.assessment.domain.vo.Skill;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/practice")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
@RequiredArgsConstructor
public class PracticeLibraryController {
    private final CurrentUserProvider currentUser;
    private final GetPracticeProgressUseCase progress;
    private final GetPracticeHistoryUseCase history;

    @GetMapping("/progress")
    public ProgressResponse progress(@RequestParam("skill") Skill skill, @RequestParam("scope") AttemptScope scope) {
        var result = progress.execute(currentUser.requireUserId(), skill, scope);
        return new ProgressResponse(result.completed().stream().map(key -> new ProgressKey(key.packageId(), key.partNo())).toList());
    }

    @GetMapping("/history")
    public HistoryResponse history(@RequestParam(name = "skill", required = false) Skill skill,
                                   @RequestParam(name = "page", defaultValue = "0") int page,
                                   @RequestParam(name = "size", defaultValue = "20") int size) {
        var result = history.execute(currentUser.requireUserId(), skill, page, size);
        return new HistoryResponse(result.totalElements(), result.page(), result.size(), result.items());
    }

    public record ProgressKey(UUID packageId, Integer partNo) {}
    public record ProgressResponse(List<ProgressKey> completed) {}
    public record HistoryResponse(long totalElements, int page, int size, List<PracticeHistoryResult.Item> items) {}
}

package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.usecase.GetPracticeTestMilestonesUseCase;
import com.ieltspath.content.application.usecase.ReplacePracticeTestMilestonesUseCase;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/admin/courses/{id}/practice-test-milestones")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class PracticeTestMilestoneController {
    private final GetPracticeTestMilestonesUseCase get;
    private final ReplacePracticeTestMilestonesUseCase replace;
    public PracticeTestMilestoneController(GetPracticeTestMilestonesUseCase get, ReplacePracticeTestMilestonesUseCase replace) {
        this.get=get; this.replace=replace;
    }
    @GetMapping public List<PracticeTestMilestone> list(@PathVariable("id") UUID id) { return get.execute(id, false); }
    @PutMapping public List<PracticeTestMilestone> replace(@PathVariable("id") UUID id, @Valid @RequestBody MilestonesRequest request) {
        return replace.execute(id, request.milestones().stream().map(m ->
                new PracticeTestMilestone(m.packageId(), null, null, null, m.afterTopicSequence())).toList());
    }
    public record MilestonesRequest(@NotNull @Size(max=100) List<@NotNull @Valid Entry> milestones) {}
    public record Entry(@NotNull UUID packageId, @Min(1) int afterTopicSequence) {}
}

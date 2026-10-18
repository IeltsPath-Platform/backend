package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.usecase.GetPracticeTestMilestonesUseCase;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/learning-content/courses/{id}/practice-test-milestones")
public class InternalPracticeTestMilestoneController {
    private final GetPracticeTestMilestonesUseCase get;
    public InternalPracticeTestMilestoneController(GetPracticeTestMilestonesUseCase get) { this.get=get; }
    @GetMapping public List<PracticeTestMilestone> list(@PathVariable("id") UUID id) { return get.execute(id, true); }
}

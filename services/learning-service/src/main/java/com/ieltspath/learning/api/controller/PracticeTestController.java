package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.TestAssignmentResponse;
import com.ieltspath.learning.application.result.PracticeTestMilestoneResult;
import com.ieltspath.learning.application.usecase.AssignPracticeTestUseCase;
import com.ieltspath.learning.application.usecase.ListPracticeTestsUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning/courses/{id}/practice-tests")
@PreAuthorize("hasRole('CUSTOMER')")
public class PracticeTestController {
    private final CurrentUserProvider currentUser;
    private final ListPracticeTestsUseCase list;
    private final AssignPracticeTestUseCase assign;
    public PracticeTestController(CurrentUserProvider currentUser,ListPracticeTestsUseCase list,AssignPracticeTestUseCase assign) {
        this.currentUser=currentUser;this.list=list;this.assign=assign;
    }
    @GetMapping public List<PracticeTestMilestoneResult> list(@PathVariable("id") UUID id) {
        return list.execute(currentUser.requireUserId(),id);
    }
    @PostMapping("/{packageId}/assignments") public TestAssignmentResponse assign(@PathVariable("id") UUID id,@PathVariable("packageId") UUID packageId) {
        return TestAssignmentResponse.from(assign.execute(currentUser.requireUserId(),id,packageId));
    }
}

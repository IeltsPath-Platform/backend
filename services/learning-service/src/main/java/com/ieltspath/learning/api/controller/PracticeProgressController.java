package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.PracticeProgressResult;
import com.ieltspath.learning.application.usecase.GetPracticeProgressUseCase;
import com.ieltspath.learning.domain.vo.PracticeProgressSkill;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The practice tests of the catalog the learner has finished in Speaking and Writing (Reading and Listening are read from
 * Assessment). Same shape as the Assessment progress, plus the graded answer to open.
 */
@RestController
@RequestMapping("/api/learning/practice-progress")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class PracticeProgressController {
    private final CurrentUserProvider currentUser;
    private final GetPracticeProgressUseCase progress;

    @GetMapping
    public PracticeProgressResult get(@RequestParam("skill") PracticeProgressSkill skill,
                                      @RequestParam("scope") SpeakingScope scope) {
        return progress.execute(currentUser.requireUserId(), skill, scope);
    }
}

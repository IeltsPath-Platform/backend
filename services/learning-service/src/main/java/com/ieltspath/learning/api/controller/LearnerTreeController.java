package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.GetLearnerTreeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearnerTreeController {
    private final CurrentUserProvider currentUser;
    private final GetLearnerTreeUseCase tree;

    @GetMapping("/tree")
    public GetLearnerTreeUseCase.Result tree(@RequestParam("courseId") UUID courseId) {
        return tree.execute(currentUser.requireUserId(), courseId);
    }
}

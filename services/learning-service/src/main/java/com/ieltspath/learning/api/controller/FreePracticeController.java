package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.usecase.StartFreePracticeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Free practice on one knowledge point. Answers are submitted through the existing practice-attempt endpoint. */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class FreePracticeController {
    private final CurrentUserProvider currentUser;
    private final StartFreePracticeUseCase start;

    @PostMapping("/knowledge-points/{id}/free-practice")
    public ResponseEntity<PracticeAttemptView> start(@PathVariable("id") UUID knowledgePointId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(start.execute(currentUser.requireUserId(), knowledgePointId));
    }
}

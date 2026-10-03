package com.group01.learning.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.dto.request.StartPracticeAttemptRequest;
import com.group01.learning.api.dto.request.SubmitExerciseRequest;
import com.group01.learning.api.dto.response.PracticeSubmissionResponse;
import com.group01.learning.application.result.LessonPracticeSetsResult;
import com.group01.learning.application.result.PracticeAttemptView;
import com.group01.learning.application.usecase.GetLessonPracticeSetsUseCase;
import com.group01.learning.application.usecase.GetPracticeAttemptUseCase;
import com.group01.learning.application.usecase.StartPracticeAttemptUseCase;
import com.group01.learning.application.usecase.SubmitPracticeAttemptUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class PracticeController {
    private final CurrentUserProvider currentUser;
    private final GetLessonPracticeSetsUseCase catalog;
    private final StartPracticeAttemptUseCase start;
    private final GetPracticeAttemptUseCase get;
    private final SubmitPracticeAttemptUseCase submit;

    @GetMapping("/lessons/{id}/practice-sets")
    public LessonPracticeSetsResult catalog(@PathVariable("id") UUID lessonId) {
        return catalog.execute(currentUser.requireUserId(), lessonId);
    }

    @PostMapping("/lessons/{id}/practice-attempts")
    public ResponseEntity<PracticeAttemptView> start(@PathVariable("id") UUID lessonId,
                                                      @Valid @RequestBody StartPracticeAttemptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(start.execute(currentUser.requireUserId(), lessonId, request.packageId()));
    }

    @GetMapping("/practice-attempts/{id}")
    public Object get(@PathVariable("id") UUID attemptId) {
        var result = get.execute(currentUser.requireUserId(), attemptId);
        return result.submission() == null ? result.view() : PracticeSubmissionResponse.from(result.submission());
    }

    @PostMapping("/practice-attempts/{id}/submissions")
    public PracticeSubmissionResponse submit(@PathVariable("id") UUID attemptId,
                                             @Valid @RequestBody SubmitExerciseRequest request) {
        return PracticeSubmissionResponse.from(submit.execute(currentUser.requireUserId(), attemptId,
                request.toCommand()));
    }
}

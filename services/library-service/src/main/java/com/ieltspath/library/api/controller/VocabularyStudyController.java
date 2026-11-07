package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.application.result.StudyAttemptResult.*;
import com.ieltspath.library.domain.study.StudyAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequiredArgsConstructor @Validated
@RequestMapping("/api/learning-support/vocabulary-study")
public class VocabularyStudyController {
    private final CurrentUserProvider currentUser;
    private final StartStudyAttemptUseCase start;
    private final GetStudyAttemptUseCase get;
    private final SubmitStudyAnswerUseCase answer;
    private final CompleteStudyAttemptUseCase complete;
    private final GetStudyProgressUseCase progress;
    public record StartRequest(@NotNull UUID requestId,@NotNull @Pattern(regexp="RESUME_OR_START|REPLAY") String mode) {}
    public record AnswerRequest(@NotNull UUID requestId,@NotNull StudyAnswer answer) {}
    public record Request(@NotNull UUID requestId) {}
    @GetMapping("/progress") public List<TopicProgress> progress(@RequestParam @Size(min=1,max=100) List<UUID> topicIds) {return progress.execute(currentUser.requireUserId(),topicIds);}
    @PostMapping("/units/{unitId}/attempts") public ResponseEntity<Attempt> start(@PathVariable UUID unitId,@Valid @RequestBody StartRequest request) {
        var result=start.execute(currentUser.requireUserId(),unitId,request.requestId(),request.mode());return ResponseEntity.status(result.created()?201:200).body(result.attempt());
    }
    @GetMapping("/attempts/{attemptId}") public Attempt get(@PathVariable UUID attemptId){return get.execute(currentUser.requireUserId(),attemptId);}
    @PostMapping("/attempts/{attemptId}/steps/{stepId}/answers") public StepResult answer(@PathVariable UUID attemptId,@PathVariable UUID stepId,@Valid @RequestBody AnswerRequest request) {
        return answer.execute(currentUser.requireUserId(),attemptId,stepId,request.requestId(),request.answer());
    }
    @PostMapping("/attempts/{attemptId}/complete") public Completion complete(@PathVariable UUID attemptId,@Valid @RequestBody Request request){return complete.execute(currentUser.requireUserId(),attemptId,request.requestId());}
}

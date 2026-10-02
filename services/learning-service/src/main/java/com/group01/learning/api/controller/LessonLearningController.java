package com.group01.learning.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.dto.request.SubmitExerciseRequest;
import com.group01.learning.api.dto.response.CompletionResponse;
import com.group01.learning.api.dto.response.LessonResponse;
import com.group01.learning.api.dto.response.MasteryResponse;
import com.group01.learning.api.dto.response.SubmissionResponse;
import com.group01.learning.api.dto.response.TopicResponse;
import com.group01.learning.application.result.TopicLessonsResult;
import com.group01.learning.application.usecase.GetMasteryUseCase;
import com.group01.learning.application.usecase.GetTopicLessonsUseCase;
import com.group01.learning.application.usecase.GetLessonUseCase;
import com.group01.learning.application.usecase.SubmitLessonExerciseUseCase;
import com.group01.learning.application.usecase.CompleteLessonUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LessonLearningController {
    private final CurrentUserProvider currentUser;
    private final RefreshLearningTopicsUseCase refreshLearningTopicsUseCase;
    private final GetTopicLessonsUseCase getTopicLessonsUseCase;
    private final GetLessonUseCase getLessonUseCase;
    private final SubmitLessonExerciseUseCase submitLessonExerciseUseCase;
    private final CompleteLessonUseCase completeLessonUseCase;
    private final GetMasteryUseCase getMasteryUseCase;

    @GetMapping("/topics")
    public List<TopicResponse> topics() {
        return refreshLearningTopicsUseCase.execute(currentUser.requireUserId()).stream()
                .map(TopicResponse::from)
                .toList();
    }

    @GetMapping("/topics/{id}/lessons")
    public TopicLessonsResult topicLessons(@PathVariable("id") UUID id) {
        return getTopicLessonsUseCase.execute(currentUser.requireUserId(), id);
    }

    @GetMapping("/lessons/{id}")
    public LessonResponse lesson(@PathVariable("id") UUID id) {
        return LessonResponse.from(getLessonUseCase.execute(currentUser.requireUserId(), id));
    }

    @PostMapping("/lessons/{id}/exercises/{blockId}/submissions")
    public SubmissionResponse submit(@PathVariable("id") UUID id, @PathVariable("blockId") UUID blockId,
                                     @Valid @RequestBody SubmitExerciseRequest request) {
        return SubmissionResponse.from(submitLessonExerciseUseCase.execute(
                currentUser.requireUserId(), id, blockId, request.toCommand()));
    }

    @PostMapping("/lessons/{id}/complete")
    public CompletionResponse complete(@PathVariable("id") UUID id) {
        return new CompletionResponse(completeLessonUseCase.execute(currentUser.requireUserId(), id), "COMPLETED");
    }

    @GetMapping("/mastery")
    public List<MasteryResponse> mastery() {
        return getMasteryUseCase.execute(currentUser.requireUserId()).stream()
                .map(MasteryResponse::from)
                .toList();
    }
}

package com.group01.learning.api.controller;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.learning.api.dto.*;
import com.group01.learning.application.usecase.GetMasteryUseCase;
import com.group01.learning.application.usecase.LearnLessonUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
public class LessonLearningController {
    private final CurrentUserProvider currentUser;
    private final RefreshLearningTopicsUseCase topics;
    private final LearnLessonUseCase lessons;
    private final GetMasteryUseCase mastery;

    public LessonLearningController(CurrentUserProvider currentUser, RefreshLearningTopicsUseCase topics,
                                    LearnLessonUseCase lessons, GetMasteryUseCase mastery) {
        this.currentUser = currentUser;
        this.topics = topics;
        this.lessons = lessons;
        this.mastery = mastery;
    }

    @GetMapping("/topics")
    public List<TopicResponse> topics() {
        return topics.execute(currentUser.requireUserId()).stream().map(TopicResponse::from).toList();
    }

    @GetMapping("/topics/{id}/lessons")
    public TopicLessonsResponse topicLessons(@PathVariable("id") UUID id) {
        return TopicLessonsResponse.from(lessons.list(currentUser.requireUserId(), id));
    }

    @GetMapping("/lessons/{id}")
    public LessonResponse lesson(@PathVariable("id") UUID id) {
        return LessonResponse.from(lessons.get(currentUser.requireUserId(), id));
    }

    @PostMapping("/lessons/{id}/exercises/{blockId}/submissions")
    public SubmissionResponse submit(@PathVariable("id") UUID id, @PathVariable("blockId") UUID blockId,
                                     @Valid @RequestBody SubmitExerciseRequest request) {
        return SubmissionResponse.from(lessons.submit(currentUser.requireUserId(), id, blockId, request.toCommand()));
    }

    @PostMapping("/lessons/{id}/complete")
    public CompletionResponse complete(@PathVariable("id") UUID id) {
        return new CompletionResponse(lessons.complete(currentUser.requireUserId(), id), "COMPLETED");
    }

    @GetMapping("/mastery")
    public List<MasteryResponse> mastery() {
        return mastery.execute(currentUser.requireUserId()).stream().map(MasteryResponse::from).toList();
    }

    public record CompletionResponse(UUID lessonId, String status) {}
}

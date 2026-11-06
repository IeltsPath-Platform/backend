package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.request.CreateLessonSpeakingSubmissionRequest;
import com.ieltspath.learning.api.dto.request.StartPracticeAttemptRequest;
import com.ieltspath.learning.api.dto.request.SubmitEssayRequest;
import com.ieltspath.learning.api.dto.request.SubmitExerciseRequest;
import com.ieltspath.learning.api.dto.request.SubmitPracticeEssayToExaminerRequest;
import com.ieltspath.learning.api.dto.response.PracticeAttemptReviewResponse;
import com.ieltspath.learning.api.dto.response.PracticeSubmissionResponse;
import com.ieltspath.learning.api.dto.response.WritingSubmissionResponse;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.LessonPracticeSetsResult;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.result.SpeakingSubmissionResult;
import com.ieltspath.learning.application.usecase.CreateLessonSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.GetLessonPracticeSetsUseCase;
import com.ieltspath.learning.application.usecase.GetPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.ReviewPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.StartPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeEssayToExaminerUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeEssayUseCase;
import com.ieltspath.learning.domain.vo.LearningSkill;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class PracticeController {
    private final CurrentUserProvider currentUser;
    private final GetLessonPracticeSetsUseCase catalog;
    private final StartPracticeAttemptUseCase start;
    private final GetPracticeAttemptUseCase get;
    private final ReviewPracticeAttemptUseCase review;
    private final SubmitPracticeAttemptUseCase submit;
    private final SubmitPracticeEssayUseCase submitEssay;
    private final SubmitPracticeEssayToExaminerUseCase submitEssayToExaminer;
    private final CreateLessonSpeakingSubmissionUseCase createSpeaking;

    @GetMapping("/lessons/{id}/practice-sets")
    public LessonPracticeSetsResult catalog(@PathVariable("id") UUID lessonId,
                                            @RequestParam(value = "skill", required = false) String skill) {
        return catalog.execute(currentUser.requireUserId(), lessonId, skill(skill));
    }

    private static Optional<LearningSkill> skill(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(LearningSkill.valueOf(value));
        } catch (IllegalArgumentException unknown) {
            throw new LearningRequestException(400, "INVALID_SKILL",
                    "skill must be LISTENING, READING, WRITING or SPEAKING");
        }
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

    @GetMapping("/practice-attempts/{id}/review")
    public PracticeAttemptReviewResponse review(@PathVariable("id") UUID attemptId) {
        return PracticeAttemptReviewResponse.from(review.execute(currentUser.requireUserId(), attemptId));
    }

    @PostMapping("/practice-attempts/{id}/essays/{questionVersionId}/submissions")
    public WritingSubmissionResponse submitEssay(@PathVariable("id") UUID attemptId,
                                                 @PathVariable("questionVersionId") UUID questionVersionId,
                                                 @Valid @RequestBody SubmitEssayRequest request) {
        return WritingSubmissionResponse.from(submitEssay.execute(currentUser.requireUserId(), attemptId,
                questionVersionId, request.requestId(), request.essayText()));
    }

    /**
     * Sends the essay to the examiner the learner chose instead of the AI. The response is the submission, status
     * AWAITING_EXAMINER; the examiner's review is read from {@code /api/learning/writing-submissions/{id}/human-review}.
     */
    @PostMapping("/practice-attempts/{id}/essays/{questionVersionId}/examiner-submissions")
    public WritingSubmissionResponse submitEssayToExaminer(@PathVariable("id") UUID attemptId,
                                                           @PathVariable("questionVersionId") UUID questionVersionId,
                                                           @Valid @RequestBody SubmitPracticeEssayToExaminerRequest request) {
        return WritingSubmissionResponse.from(submitEssayToExaminer.execute(currentUser.requireUserId(), attemptId,
                questionVersionId, request.requestId(), request.essayText(), request.examinerId()));
    }

    /**
     * Opens the recorded answer of the attempt (points are checked, nothing is charged). The recording is then stored and
     * completed through {@code /api/learning/speaking-practice/submissions/{id}/...}; the grade reaches mastery only when
     * the attempt is submitted.
     */
    @PostMapping("/practice-attempts/{id}/speaking-submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SpeakingSubmissionResult createSpeaking(@PathVariable("id") UUID attemptId,
                                                   @Valid @RequestBody CreateLessonSpeakingSubmissionRequest request) {
        return createSpeaking.execute(currentUser.requireUserId(), attemptId, request.requestId(), request.consent());
    }

    @PostMapping("/practice-attempts/{id}/submissions")
    public PracticeSubmissionResponse submit(@PathVariable("id") UUID attemptId,
                                             @Valid @RequestBody SubmitExerciseRequest request) {
        return PracticeSubmissionResponse.from(submit.execute(currentUser.requireUserId(), attemptId,
                request.toCommand()));
    }
}

package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CreateGradingJobRequest;
import com.ieltspath.assessment.api.dto.response.GradingJobResponse;
import com.ieltspath.assessment.application.command.CreateGradingJobCommand;
import com.ieltspath.assessment.application.usecase.CreateGradingJobUseCase;
import com.ieltspath.assessment.application.usecase.GetGradingJobUseCase;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.application.usecase.ListGradingQueueUseCase;
import com.ieltspath.assessment.application.usecase.ClaimGradingJobUseCase;
import com.ieltspath.assessment.application.usecase.GetJobEssayUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api/assessments/grading-jobs")
@RequiredArgsConstructor
public class GradingJobController {

    private final CurrentUserProvider currentUser;
    private final GetGradingJobUseCase getGradingJobUseCase;
    private final CreateGradingJobUseCase createGradingJobUseCase;
    private final ListGradingQueueUseCase listQueue;
    private final ClaimGradingJobUseCase claimJob;
    private final GetJobEssayUseCase getEssay;

    @GetMapping
    @PreAuthorize("hasRole('EXAMINER')")
    public ExaminerQueueStore.QueuePage list(@RequestParam(name="mode",defaultValue="HUMAN") String mode,
            @RequestParam(name="status",defaultValue="QUEUED") String status,@RequestParam(name="page",defaultValue="0") int page,
            @RequestParam(name="size",defaultValue="20") int size) {
        // An examiner sees only the jobs learners chose them for. Administrators are not granted this endpoint.
        return listQueue.executeFor(currentUser.requireUserId(),mode,status,page,size);
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasRole('EXAMINER')")
    public ExaminerQueueStore.QueueEntry claim(@PathVariable("id") UUID id) {
        return claimJob.execute(currentUser.requireUserId(),id);
    }

    @GetMapping("/{id}/essay")
    @PreAuthorize("hasRole('EXAMINER')")
    public ExaminerQueueStore.Essay essay(@PathVariable("id") UUID id) {
        return getEssay.execute(currentUser.requireUserId(),id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradingJobResponse create(@Valid @RequestBody CreateGradingJobRequest request) {
        var result = createGradingJobUseCase.execute(new CreateGradingJobCommand(
                currentUser.requireUserId(),
                request.submissionId(),
                request.skill(),
                request.gradingMode(),
                request.pointCostSnapshot(),
                request.idempotencyKey(),
                request.examinerId()
        ));
        return GradingJobResponse.from(result);
    }

    @GetMapping("/{id}")
    public GradingJobResponse get(@PathVariable("id") UUID id) {
        return GradingJobResponse.from(getGradingJobUseCase.execute(currentUser.requireUserId(), id));
    }
}

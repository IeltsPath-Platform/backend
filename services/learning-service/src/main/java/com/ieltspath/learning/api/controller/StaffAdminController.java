package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.StaffInsightResults;
import com.ieltspath.learning.application.usecase.StaffInsightUseCase;
import com.ieltspath.learning.application.usecase.StaffWorkUseCase;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagStatus;
import com.ieltspath.learning.domain.vo.StaffRecords.LearnerFlag;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The staff area of Learning: learner overview, anomaly flags, work queues, and statistics for content authors. An
 * Examiners work the examiner queue, and content authors work the content queue.
 */
@RestController
@RequestMapping("/api/learning")
public class StaffAdminController {
    private final CurrentUserProvider currentUser;
    private final StaffInsightUseCase insight;
    private final StaffWorkUseCase work;

    public StaffAdminController(CurrentUserProvider currentUser, StaffInsightUseCase insight, StaffWorkUseCase work) {
        this.currentUser = currentUser;
        this.insight = insight;
        this.work = work;
    }

    @GetMapping("/admin/learners/{userId}/overview")
    @PreAuthorize("hasRole('EXAMINER')")
    public StaffInsightResults.Overview overview(@PathVariable("userId") UUID userId) {
        return insight.overview(userId);
    }

    @GetMapping("/admin/work-items")
    @PreAuthorize("hasAnyRole('EXAMINER', 'CONTENT_AUTHOR')")
    public List<WorkItem> workItems(@RequestParam(name = "queue", required = false) Queue queue,
                                    @RequestParam(name = "status", required = false) WorkStatus status,
                                    Authentication authentication) {
        return work.list(queues(authentication), queue, status);
    }

    @PatchMapping("/admin/work-items/{id}")
    @PreAuthorize("hasAnyRole('EXAMINER', 'CONTENT_AUTHOR')")
    public WorkItem updateWorkItem(@PathVariable("id") UUID id, @RequestBody UpdateWorkItemRequest request,
                                   Authentication authentication) {
        return work.update(queues(authentication), currentUser.requireUserId(), id,
                new StaffWorkUseCase.UpdateWork(request.status(), request.action(), request.result(), request.note(),
                        request.noteVisibleToLearner()));
    }

    @PatchMapping("/admin/flags/{id}")
    @PreAuthorize("hasRole('EXAMINER')")
    public LearnerFlag reviewFlag(@PathVariable("id") UUID id, @RequestBody ReviewFlagRequest request) {
        return work.reviewFlag(currentUser.requireUserId(), id, request.status(), request.note());
    }

    @GetMapping("/admin/question-stats")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public StaffInsightResults.QuestionStats questionStats(
            @RequestParam(name = "minAttempts", defaultValue = "20") int minAttempts) {
        return insight.questionStats(minAttempts);
    }

    @GetMapping("/admin/tree-quality")
    @PreAuthorize("hasRole('CONTENT_AUTHOR')")
    public StaffInsightResults.TreeQuality treeQuality(@RequestParam(name = "courseId", required = false) UUID courseId) {
        return insight.treeQuality(courseId);
    }

    /** Notes staff chose to show the learner. */
    @GetMapping("/notes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<LearnerNote> notes() {
        return work.notesFor(currentUser.requireUserId()).stream()
                .map(item -> new LearnerNote(item.id(), item.kind().name(), item.note(), item.updatedAt())).toList();
    }

    /** The queues the caller's roles give access to. */
    private static Set<Queue> queues(Authentication authentication) {
        Set<Queue> queues = EnumSet.noneOf(Queue.class);
        for (var authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_EXAMINER" -> queues.add(Queue.EXAMINER);
                case "ROLE_CONTENT_AUTHOR" -> queues.add(Queue.CONTENT_AUTHOR);
                default -> { }
            }
        }
        return queues;
    }

    public record UpdateWorkItemRequest(WorkStatus status, String action, String result, String note,
                                        Boolean noteVisibleToLearner) {}

    public record ReviewFlagRequest(@NotNull FlagStatus status, String note) {}

    public record LearnerNote(UUID id, String kind, String note, java.time.Instant at) {}
}

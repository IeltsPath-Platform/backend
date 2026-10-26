package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.WorkItemView;
import com.ieltspath.learning.application.service.WorkItemSubjectResolver;
import com.ieltspath.learning.application.service.WorkItemSync;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagStatus;
import com.ieltspath.learning.domain.vo.StaffRecords.LearnerFlag;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The staff work queues. An Examiner sees the Examiner queue, R&D the content queue, an Admin both; the caller passes
 * the queues their role allows. Listing syncs first, so the queue is current without a background job.
 */
@Service
public class StaffWorkUseCase {
    private final StaffInsightRepository insight;
    private final WorkItemSync sync;
    private final WorkItemSubjectResolver subjects;

    public StaffWorkUseCase(StaffInsightRepository insight, WorkItemSync sync, WorkItemSubjectResolver subjects) {
        this.insight = insight;
        this.sync = sync;
        this.subjects = subjects;
    }

    @Transactional
    public List<WorkItem> list(Set<Queue> allowed, Queue requested, WorkStatus status) {
        return listItems(allowed, requested, status, null);
    }

    /** The same list with what each content item is about, named by one call to Content. */
    @Transactional
    public List<WorkItemView> listWithSubjects(Set<Queue> allowed, Queue requested, WorkStatus status, WorkKind kind) {
        List<WorkItem> items = listItems(allowed, requested, status, kind);
        Map<UUID, Map<String, Object>> named = subjects.resolve(items);
        return items.stream().map(item -> new WorkItemView(item, named.get(item.id()))).toList();
    }

    private List<WorkItem> listItems(Set<Queue> allowed, Queue requested, WorkStatus status, WorkKind kind) {
        if (requested != null && !allowed.contains(requested)) {
            throw new LearningRequestException(403, "QUEUE_NOT_ALLOWED", "This queue is not yours to see");
        }
        sync.sync();
        return insight.workItems(requested == null ? allowed : Set.of(requested), status, kind);
    }

    /** Takes the item, records what was done and the result, and moves its status. */
    @Transactional
    public WorkItem update(Set<Queue> allowed, UUID staffId, UUID itemId, UpdateWork change) {
        WorkItem item = insight.findWorkItem(itemId).filter(found -> allowed.contains(found.queue()))
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Work item was not found"));
        WorkStatus status = change.status() == null ? item.status() : change.status();
        UUID assignee = status == WorkStatus.OPEN ? null : item.assigneeId() != null ? item.assigneeId() : staffId;
        boolean visible = change.noteVisibleToLearner() == null ? item.noteVisibleToLearner()
                : change.noteVisibleToLearner();
        String note = change.note() == null ? item.note() : change.note();
        if (visible && (note == null || note.isBlank())) {
            throw new LearningRequestException(422, "NOTE_REQUIRED", "A note shown to the learner cannot be empty");
        }
        if (visible && item.subjectUserId() == null) {
            throw new LearningRequestException(422, "NO_LEARNER", "This item has no learner to show a note to");
        }
        insight.updateWorkItem(itemId, status, assignee, change.action() == null ? item.action() : change.action(),
                change.result() == null ? item.result() : change.result(), note, visible);
        return insight.findWorkItem(itemId).orElseThrow();
    }

    /** Confirms or dismisses a learner flag, with a note. */
    @Transactional
    public LearnerFlag reviewFlag(UUID staffId, UUID flagId, FlagStatus status, String note) {
        if (status == FlagStatus.OPEN) {
            throw new LearningRequestException(422, "INVALID_FLAG_STATUS", "A flag is confirmed or dismissed");
        }
        insight.findFlag(flagId).orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Flag was not found"));
        insight.reviewFlag(flagId, status, staffId, note);
        return insight.findFlag(flagId).orElseThrow();
    }

    /** The notes staff chose to show this learner. */
    @Transactional(readOnly = true)
    public List<WorkItem> notesFor(UUID userId) {
        return insight.visibleNotes(userId);
    }

    public record UpdateWork(WorkStatus status, String action, String result, String note,
                             Boolean noteVisibleToLearner) {}
}

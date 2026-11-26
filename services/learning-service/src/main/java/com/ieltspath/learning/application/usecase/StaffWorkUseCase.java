package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.result.WorkItemView;
import com.ieltspath.learning.application.result.WorkItemPage;
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

    @Transactional
    public WorkItemPage listPageWithSubjects(Set<Queue> allowed, Queue requested, WorkStatus status, WorkKind kind,
                                             boolean activeOnly, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size) {
            throw new LearningRequestException(400, "INVALID_PAGE", "Thông số phân trang không hợp lệ.");
        }
        Set<Queue> queues = queues(allowed, requested);
        sync.sync();
        int offset = page * size;
        long total = insight.countWorkItems(queues, status, kind, activeOnly);
        List<WorkItem> items = insight.workItems(queues, status, kind, activeOnly, offset, size);
        Map<UUID, Map<String, Object>> named = subjects.resolve(items);
        List<WorkItemView> content = items.stream().map(item -> new WorkItemView(item, named.get(item.id()))).toList();
        return new WorkItemPage(content, page, size, total, (int) ((total + size - 1) / size));
    }

    private static Set<Queue> queues(Set<Queue> allowed, Queue requested) {
        if (requested != null && !allowed.contains(requested)) {
            throw new LearningRequestException(403, "QUEUE_NOT_ALLOWED", "Bạn không có quyền xem hàng đợi này.");
        }
        return requested == null ? allowed : Set.of(requested);
    }

    private List<WorkItem> listItems(Set<Queue> allowed, Queue requested, WorkStatus status, WorkKind kind) {
        if (requested != null && !allowed.contains(requested)) {
            throw new LearningRequestException(403, "QUEUE_NOT_ALLOWED", "Bạn không có quyền xem hàng chờ này.");
        }
        sync.sync();
        return insight.workItems(requested == null ? allowed : Set.of(requested), status, kind);
    }

    /** Takes the item, records what was done and the result, and moves its status. */
    @Transactional
    public WorkItem update(Set<Queue> allowed, UUID staffId, UUID itemId, UpdateWork change) {
        WorkItem item = insight.findWorkItem(itemId).filter(found -> allowed.contains(found.queue()))
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy đầu việc."));
        WorkStatus status = change.status() == null ? item.status() : change.status();
        UUID assignee = status == WorkStatus.OPEN ? null : item.assigneeId() != null ? item.assigneeId() : staffId;
        boolean visible = change.noteVisibleToLearner() == null ? item.noteVisibleToLearner()
                : change.noteVisibleToLearner();
        String note = change.note() == null ? item.note() : change.note();
        if (visible && (note == null || note.isBlank())) {
            throw new LearningRequestException(422, "NOTE_REQUIRED", "Ghi chú hiển thị cho học viên không được để trống.");
        }
        if (visible && item.subjectUserId() == null) {
            throw new LearningRequestException(422, "NO_LEARNER", "Đầu việc này không có học viên để gửi ghi chú.");
        }
        insight.updateWorkItem(itemId, status, assignee, change.action() == null ? item.action() : change.action(),
                change.result() == null ? item.result() : change.result(), note, visible);
        return insight.findWorkItem(itemId).orElseThrow();
    }

    /** Confirms or dismisses a learner flag, with a note. */
    @Transactional
    public LearnerFlag reviewFlag(UUID staffId, UUID flagId, FlagStatus status, String note) {
        if (status == FlagStatus.OPEN) {
            throw new LearningRequestException(422, "INVALID_FLAG_STATUS", "Trạng thái cờ chỉ có thể là xác nhận hoặc bác bỏ.");
        }
        insight.findFlag(flagId).orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Không tìm thấy cờ cảnh báo."));
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

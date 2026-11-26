package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository.Entry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The learner's submissions, newest first, one page at a time: what was submitted inside a course ({@link Scope#COURSE}), the
 * free practice essays or speaking of the practice bank ({@link Scope#FREE}), or the free practice sets on one knowledge point
 * ({@link Scope#KNOWLEDGE}). Free practice Listening and Reading are held by Assessment and listed there. A page costs one
 * query; names of lessons, topics and tests are not resolved here. A knowledge point is named from the catalog, read once and
 * only for a page that has one; when the catalog does not answer, the line keeps its id and no name.
 */
@Service
public class ListSubmissionHistoryUseCase {
    public static final int MAX_PAGE_SIZE = 50;

    public enum Scope { COURSE, FREE, KNOWLEDGE }

    private final SubmissionHistoryRepository history;
    private final KnowledgePointDirectory directory;

    public ListSubmissionHistoryUseCase(SubmissionHistoryRepository history, KnowledgePointDirectory directory) {
        this.history = history;
        this.directory = directory;
    }

    /** @param skill WRITING or SPEAKING; required for {@link Scope#FREE} and ignored otherwise */
    public Page execute(UUID userId, Scope scope, String skill, int page, int size) {
        if (scope == Scope.FREE && !"WRITING".equals(skill) && !"SPEAKING".equals(skill)) {
            throw new IllegalArgumentException("Kỹ năng luyện tập tự do phải là WRITING hoặc SPEAKING.");
        }
        int pageNo = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int limit = pageSize + 1;
        int offset = pageNo * pageSize;
        // One more than the page tells whether there is a next page, without counting every row.
        List<Entry> rows = switch (scope) {
            case COURSE -> history.findCourse(userId, limit, offset);
            case FREE -> history.findFree(userId, skill, limit, offset);
            case KNOWLEDGE -> history.findKnowledgePractice(userId, limit, offset);
        };
        boolean hasNext = rows.size() > pageSize;
        List<Entry> shown = hasNext ? rows.subList(0, pageSize) : rows;
        return new Page(scope == Scope.KNOWLEDGE ? named(shown) : shown, pageNo, pageSize, hasNext);
    }

    private List<Entry> named(List<Entry> rows) {
        if (rows.isEmpty()) return rows;
        Map<UUID, KnowledgePointDirectory.Entry> catalog;
        try {
            catalog = directory.describe();
        } catch (RuntimeException unavailable) {
            // The list is still worth showing without names; the client falls back to a plain label.
            return rows;
        }
        return rows.stream().map(row -> {
            var point = catalog.get(row.knowledgePointId());
            return point == null ? row : row.withKnowledgePointName(point.code(), point.name());
        }).toList();
    }

    public record Page(List<Entry> items, int page, int size, boolean hasNext) {}
}

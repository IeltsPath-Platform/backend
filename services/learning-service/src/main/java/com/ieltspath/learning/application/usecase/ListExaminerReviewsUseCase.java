package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository.Requested;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * The essays a learner sent to an examiner, newest first, one page at a time. It lists only what Learning holds (which essay,
 * which review in Assessment); whether the examiner finished and the band come from Assessment's list of the learner's jobs
 * (matched by {@code reviewSubmissionId}), so a page costs no call per essay.
 */
@Service
public class ListExaminerReviewsUseCase {
    public static final int MAX_PAGE_SIZE = 50;

    private final HumanReviewRequestRepository requests;

    public ListExaminerReviewsUseCase(HumanReviewRequestRepository requests) {
        this.requests = requests;
    }

    public Page execute(UUID userId, int page, int size) {
        int pageNo = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        // One more than the page tells whether there is a next page, without counting every row.
        List<Requested> rows = requests.findByUser(userId, pageSize + 1, pageNo * pageSize);
        boolean hasNext = rows.size() > pageSize;
        return new Page(hasNext ? rows.subList(0, pageSize) : rows, pageNo, pageSize, hasNext);
    }

    public record Page(List<Requested> items, int page, int size, boolean hasNext) {}
}

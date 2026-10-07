package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.ReviewListEntry;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListReviewsUseCase {
    private final ReviewItemRepository reviews;

    public ListReviewsUseCase(ReviewItemRepository reviews) { this.reviews = reviews; }

    @Transactional(readOnly = true)
    public List<ReviewListEntry> execute(UUID userId, ReviewStatus status, LearningSkill skill, int limit) {
        return reviews.list(userId, status, skill, limit);
    }
}

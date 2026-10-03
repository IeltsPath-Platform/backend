package com.group01.learning.application.usecase;

import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.ReviewListEntry;
import com.group01.learning.domain.vo.ReviewStatus;
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

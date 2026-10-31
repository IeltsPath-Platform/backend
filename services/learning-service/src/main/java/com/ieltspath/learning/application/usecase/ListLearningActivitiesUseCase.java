package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.ApplicationSupport;
import com.ieltspath.learning.application.query.PageQuery;
import com.ieltspath.learning.application.result.LearningActivityResult;
import com.ieltspath.learning.application.result.PageResult;
import com.ieltspath.learning.domain.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListLearningActivitiesUseCase {
    private final LearningActivityRepository repository;

    @Transactional(readOnly = true)
    public PageResult<LearningActivityResult> execute(UUID userId, PageQuery query) {
        return ApplicationSupport.page(repository.findByUserId(userId, query.page(), query.size()), query)
                .map(LearningActivityResult::from);
    }
}

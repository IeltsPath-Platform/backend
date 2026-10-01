package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.LearningActivityResult;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.domain.repository.LearningActivityRepository;
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

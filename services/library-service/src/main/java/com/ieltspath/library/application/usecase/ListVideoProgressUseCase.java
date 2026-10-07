package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.application.result.VideoLearningProgressResult;
import com.ieltspath.library.domain.repository.VideoLearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListVideoProgressUseCase {
    private final VideoLearningProgressRepository repository;

    @Transactional(readOnly = true)
    public PageResult<VideoLearningProgressResult> execute(UUID userId, PageQuery query) {
        return ApplicationSupport.page(repository.findByUserId(userId, query.page(), query.size()), query)
                .map(VideoLearningProgressResult::from);
    }
}

package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.FlashcardResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListFlashcardsUseCase {
    private final FlashcardRepository repository;

    @Transactional(readOnly = true)
    public PageResult<FlashcardResult> execute(UUID userId, LibraryStatus status, PageQuery query) {
        return ApplicationSupport.page(
                repository.findByUserIdAndStatus(userId, status, query.page(), query.size()),
                query
        ).map(FlashcardResult::from);
    }
}

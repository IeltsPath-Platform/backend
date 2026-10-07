package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.FlashcardDeckResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.repository.FlashcardDeckRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListFlashcardDecksUseCase {
    private final FlashcardDeckRepository repository;

    @Transactional(readOnly = true)
    public PageResult<FlashcardDeckResult> execute(UUID userId, LibraryStatus status, PageQuery query) {
        return ApplicationSupport.page(
                repository.findByUserIdAndStatus(userId, status, query.page(), query.size()),
                query
        ).map(FlashcardDeckResult::from);
    }
}

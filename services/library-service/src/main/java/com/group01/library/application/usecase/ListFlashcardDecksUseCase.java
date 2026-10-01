package com.group01.library.application.usecase;

import com.group01.library.application.ApplicationSupport;
import com.group01.library.application.query.PageQuery;
import com.group01.library.application.result.FlashcardDeckResult;
import com.group01.library.application.result.PageResult;
import com.group01.library.domain.repository.FlashcardDeckRepository;
import com.group01.library.domain.vo.LibraryStatus;
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

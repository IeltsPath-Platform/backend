package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.FlashcardResult;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.domain.repository.FlashcardRepository;
import com.group01.learningsupport.domain.vo.LibraryStatus;
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

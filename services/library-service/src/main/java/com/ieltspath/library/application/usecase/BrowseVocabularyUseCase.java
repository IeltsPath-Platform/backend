package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrowseVocabularyUseCase {
    private final VocabularyRepository vocabulary;
    @Transactional(readOnly = true)
    public PageResult<VocabularyItem> execute(String query, int page, int size) {
        var result = vocabulary.browseActive(query == null ? "" : query.trim(), page, size);
        return new PageResult<>(result.items(), page, size, result.total());
    }
}

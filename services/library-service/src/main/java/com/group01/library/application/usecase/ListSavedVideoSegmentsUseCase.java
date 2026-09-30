package com.group01.library.application.usecase;

import com.group01.library.application.ApplicationSupport;
import com.group01.library.application.query.PageQuery;
import com.group01.library.application.result.PageResult;
import com.group01.library.application.result.SavedVideoSegmentResult;
import com.group01.library.domain.repository.SavedVideoSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListSavedVideoSegmentsUseCase {
    private final SavedVideoSegmentRepository repository;

    @Transactional(readOnly = true)
    public PageResult<SavedVideoSegmentResult> execute(UUID userId, PageQuery query) {
        return ApplicationSupport.page(repository.findByUserId(userId, query.page(), query.size()), query)
                .map(SavedVideoSegmentResult::from);
    }
}

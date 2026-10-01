package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.result.NoteResult;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.domain.repository.NoteRepository;
import com.group01.learningsupport.domain.exception.InvalidDataException;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.NoteSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListNotesUseCase {
    private final NoteRepository repository;

    @Transactional(readOnly = true)
    public PageResult<NoteResult> execute(UUID userId, LibraryStatus status, PageQuery query) {
        return ApplicationSupport.page(
                repository.findByUserIdAndStatus(userId, status, query.page(), query.size()),
                query
        ).map(NoteResult::from);
    }

    @Transactional(readOnly = true)
    public PageResult<NoteResult> execute(UUID userId, LibraryStatus status, PageQuery query,
                                          NoteSourceType sourceType, UUID sourceReferenceId) {
        if (sourceType == null) {
            if (sourceReferenceId != null) {
                throw new InvalidDataException("sourceType is required when sourceReferenceId is present");
            }
            return execute(userId, status, query);
        }
        return ApplicationSupport.page(
                sourceReferenceId == null
                        ? repository.findByUserIdAndStatusAndSourceType(
                                userId, status, sourceType, query.page(), query.size())
                        : repository.findByUserIdAndStatusAndSourceTypeAndSourceReferenceId(
                                userId, status, sourceType, sourceReferenceId, query.page(), query.size()),
                query
        ).map(NoteResult::from);
    }
}

package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.NoteResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.domain.repository.NoteRepository;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.NoteSourceType;
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

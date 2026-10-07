package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.domain.repository.SavedVideoSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSavedVideoSegmentUseCase {
    private final SavedVideoSegmentRepository repository;

    @Transactional
    public void execute(UUID userId, UUID segmentId) {
        repository.delete(ApplicationSupport.required(repository.findByIdAndUserId(segmentId, userId)));
    }
}

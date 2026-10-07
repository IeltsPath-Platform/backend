package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.CreateSavedVideoSegmentCommand;
import com.ieltspath.library.application.result.SavedVideoSegmentResult;
import com.ieltspath.library.domain.aggregate.SavedVideoSegment;
import com.ieltspath.library.domain.repository.SavedVideoSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSavedVideoSegmentUseCase {
    private final SavedVideoSegmentRepository repository;

    @Transactional
    public SavedVideoSegmentResult execute(CreateSavedVideoSegmentCommand command) {
        return SavedVideoSegmentResult.from(repository.save(SavedVideoSegment.create(
                command.userId(),
                command.videoId(),
                command.segmentId(),
                command.transcriptSnapshot(),
                command.note()
        )));
    }
}

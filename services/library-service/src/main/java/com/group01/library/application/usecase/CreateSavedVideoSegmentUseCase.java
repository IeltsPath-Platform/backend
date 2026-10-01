package com.group01.library.application.usecase;

import com.group01.library.application.command.CreateSavedVideoSegmentCommand;
import com.group01.library.application.result.SavedVideoSegmentResult;
import com.group01.library.domain.aggregate.SavedVideoSegment;
import com.group01.library.domain.repository.SavedVideoSegmentRepository;
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

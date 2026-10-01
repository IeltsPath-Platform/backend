package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.command.CreateSavedVideoSegmentCommand;
import com.group01.learningsupport.application.result.SavedVideoSegmentResult;
import com.group01.learningsupport.domain.aggregate.SavedVideoSegment;
import com.group01.learningsupport.domain.repository.SavedVideoSegmentRepository;
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

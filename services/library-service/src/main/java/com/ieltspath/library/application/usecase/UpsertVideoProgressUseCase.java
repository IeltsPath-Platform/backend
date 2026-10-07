package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.UpsertVideoProgressCommand;
import com.ieltspath.library.application.result.VideoLearningProgressResult;
import com.ieltspath.library.domain.aggregate.VideoLearningProgress;
import com.ieltspath.library.domain.repository.VideoLearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpsertVideoProgressUseCase {
    private final VideoLearningProgressRepository repository;

    @Transactional
    public VideoLearningProgressResult execute(UpsertVideoProgressCommand command) {
        VideoLearningProgress progress = repository.findByUserIdAndVideoId(command.userId(), command.videoId())
                .map(existing -> {
                    existing.replace(
                            command.lastPositionMs(),
                            command.watchedDurationSeconds(),
                            command.progressPercent(),
                            command.status(),
                            command.startedAt(),
                            command.lastWatchedAt(),
                            command.completedAt()
                    );
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(VideoLearningProgress.create(
                        command.userId(),
                        command.videoId(),
                        command.lastPositionMs(),
                        command.watchedDurationSeconds(),
                        command.progressPercent(),
                        command.status(),
                        command.startedAt(),
                        command.lastWatchedAt(),
                        command.completedAt()
                )));
        return VideoLearningProgressResult.from(progress);
    }
}

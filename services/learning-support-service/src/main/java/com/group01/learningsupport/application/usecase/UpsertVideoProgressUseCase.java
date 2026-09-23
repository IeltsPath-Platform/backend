package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.command.UpsertVideoProgressCommand;
import com.group01.learningsupport.domain.aggregate.VideoLearningProgress;
import com.group01.learningsupport.domain.repository.VideoLearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpsertVideoProgressUseCase {
    private final VideoLearningProgressRepository repository;

    @Transactional
    public VideoLearningProgress execute(UpsertVideoProgressCommand command) {
        return repository.findByUserIdAndVideoId(command.userId(), command.videoId())
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
    }
}

package com.group01.content.application.usecase;

import com.group01.content.application.command.GetGameContentSnapshotCommand;
import com.group01.content.application.result.GameContentSnapshotResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.entity.QuestionVersion;
import com.group01.content.domain.repository.QuestionRepository;
import com.group01.content.domain.vo.PublicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetGameContentSnapshotUseCase {
    private final QuestionRepository questionRepository;

    public GetGameContentSnapshotUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public GameContentSnapshotResult execute(GetGameContentSnapshotCommand command) {
        if (new HashSet<>(command.contentIds()).size() != command.contentIds().size()) {
            throw new IllegalArgumentException("contentIds must not contain duplicates");
        }
        return switch (command.learningDomain()) {
            case "GRAMMAR" -> grammarSnapshot(command.gameType(), command.contentIds());
            default -> throw new IllegalArgumentException("content-service supports GRAMMAR snapshots only");
        };
    }

    private GameContentSnapshotResult grammarSnapshot(String gameType, List<UUID> ids) {
        if (!List.of("SENTENCE_COMPLETION", "ERROR_CORRECTION", "WORD_ORDER").contains(gameType)) {
            throw new IllegalArgumentException("gameType is unsupported for grammar");
        }
        List<Question> questions = questionRepository.findByIds(ids);
        if (questions.size() != ids.size()) throw new IllegalArgumentException("One or more questions do not exist");
        List<GameContentSnapshotResult.GameContentItem> snapshots = new ArrayList<>(questions.size());
        for (Question question : questions) {
            UUID versionId = question.getCurrentPublishedVersionId();
            if (question.getStatus() != PublicationStatus.PUBLISHED || versionId == null) continue;
            question.getVersions().stream()
                    .filter(version -> version.getId().equals(versionId)
                            && version.getStatus() == PublicationStatus.PUBLISHED)
                    .findFirst()
                    .ifPresent(version -> snapshots.add(toSnapshot(question, version)));
        }
        return new GameContentSnapshotResult(snapshots);
    }

    private GameContentSnapshotResult.GameContentItem toSnapshot(Question question, QuestionVersion version) {
        return new GameContentSnapshotResult.GameContentItem(
                question.getId(), null, version.getId(), version.getStem(), version.getOptions(),
                version.getAnswerSpecJson(), version.getExplanation()
        );
    }

}
